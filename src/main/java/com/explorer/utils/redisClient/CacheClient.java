package com.explorer.utils.redisClient;

import static com.explorer.utils.constants.RedisConstants.CACHE_NULL_TTL;
import static com.explorer.utils.constants.RedisConstants.LOCK_SHOP_KEY;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CacheClient {

  //thread pool .for performance.don`t need repeat create & destroy with IO.
  private static final ExecutorService CACHE_REBUILD_EXECUTOR =
      Executors.newFixedThreadPool(10);

  private final StringRedisTemplate stringRedisTemplate;

  public CacheClient(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  private static <R> R deserialize(Class<R> type, RedisData redisData) {
    return JSONUtil.toBean((JSONObject) redisData.getData(), type);
  }

  private static RedisData deserialize(String json) {
    return JSONUtil.toBean(json, RedisData.class);
  }

  private static <R> R getBean(Class<R> type, String shopJson) {
    return JSONUtil.toBean(shopJson, type);
  }

  /**
   * use mutex to  make parallelization  to serialization, also built in with passThrough
   * protection.(set null obj)
   */
  public <R, ID> R queryWithMutex(
      String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long duration,
      TimeUnit unit) {
    String key = keyPrefix + id;
    String json = redisGet(key);    // 1.从redis查询
    if (StrUtil.isNotBlank(json)) {    // 2.判断是否存在
      return getBean(type, json);
    }
    if (json != null) {    // 判断命中的是否是空值
      return null;
    }
    String lockKey = LOCK_SHOP_KEY + id;    // 4.实现缓存重建
    R ret;    // 4.1.获取互斥锁
    try {
      boolean isLock = tryLock(lockKey);      // 4.2.判断是否获取成功

      if (!isLock) {
        Thread.sleep(50);        // 4.3.获取锁失败，休眠并重试
        return queryWithMutex(keyPrefix, id, type, dbFallback, duration,
            unit);//goto the beginning to query redis.
      }
      ret = dbFallback.apply(id);      // 4.4.获取锁成功，根据id查询数据库
      Thread.sleep(
          200);//simulate remote call DB via network.time longer ,need higher reliable of mutex
      if (ret == null) {      // 5.不存在，返回错误
        redisSet(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);        // 将空值写入redis
        return null;
      }
      redisSet(key, ret, duration, unit);      // 6.存在，写入redis
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      unlock(lockKey);      // 7.释放锁
    }
    return ret;    // 8.返回
  }

  public <R, ID>
  R queryWithLogicalExpire(
      String keyPrefix, ID id,//id可能是Long或者String都有可能
      Class<R> type, Function<ID, R> dbFallback,
      Long duration, TimeUnit unit) {
    String key = keyPrefix + id;
    String json = redisGet(key);    // 1.从redis查询缓存
    if (StrUtil.isBlank(json)) {    // 2.判断是否存在
      return null;      // 3.不存在，直接返回
    }
    RedisData redisData = deserialize(json);
    R ret = deserialize(type, redisData);
    LocalDateTime expireTime = redisData.getExpireTime();
    if (expireTime.isAfter(LocalDateTime.now())) {    // 5.判断是否过期
      return ret;      // 5.1.未过期，直接返回店铺信息
    }
    // 6.缓存重建
    String lockKey = LOCK_SHOP_KEY + id;    // 5.2.已过期，需要尝试缓存重建
    boolean isLock = tryLock(lockKey);    // 6.1.获取互斥锁
    if (isLock) {    // 6.2.判断是否获取锁成功
      CACHE_REBUILD_EXECUTOR.submit(() -> {      // 6.3.成功，开启独立线程，实现缓存重建
        try {
          R newRet = dbFallback.apply(id);          // 查询数据库
          setWithLogicalExpire(key, newRet, duration, unit);          // 重建缓存
        } catch (Exception e) {
          throw new RuntimeException(e);
        } finally {
          unlock(lockKey);          // 释放锁
        }
      });
    }
    return ret;    // 6.4.未获取到锁,返回过期的信息
  }

  public <R, ID> R queryWithPassThrough(
      String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback,
      Long duration, TimeUnit unit) {
    String key = keyPrefix + id;
    String json = redisGet(key);    // 1.从redis查询商铺缓存
    if (StrUtil.isNotBlank(json)) {    // 2.判断是否存在
      return JSONUtil.toBean(json, type);      // 3.存在，直接返回
    }
    if (json != null) {    // 判断命中的是否是空值
      return null;      // 返回一个错误信息
    }
    R ret = dbFallback.apply(id);    // 4.不存在，根据id查询数据库,一个HoF的实例
    if (ret == null) {    // 5.不存在，返回错误
      redisSet(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);// 将空值写入redis,防止骇客一直查询数据库造成数据库宕机
      return null;
    }
    this.redisSet(key, ret, duration, unit);    // 6.存在，写入redis
    return ret;
  }

  /**
   * Redis get
   */
  private String redisGet(String key) {
    return stringRedisTemplate.opsForValue().get(key);
  }

  /**
   * thread-safe mutex .
   *
   * @param key key
   * @return get lock or not
   */
  private boolean tryLock(String key) {
    //Redis-command: setnx k v  (if key not exist)
    return setnx(key);
  }

  private Boolean setnx(String key) {
    return BooleanUtil.isTrue(stringRedisTemplate.opsForValue().
        setIfAbsent(key, "1", 10, TimeUnit.SECONDS));
  }

  private void unlock(String key) {
    redisDelete(key);
  }

  private void redisDelete(String key) {
    stringRedisTemplate.delete(key);
  }

  private void redisSet(String key, Object value, Long duration, TimeUnit unit) {
    stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), duration, unit);
  }

  private void setWithLogicalExpire(String key, Object value, Long duration, TimeUnit unit) {
    RedisData redisData = new RedisData();    // 设置逻辑过期
    redisData.setData(value);
    redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(duration)));
    stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));    // 写入Redis
  }

}
