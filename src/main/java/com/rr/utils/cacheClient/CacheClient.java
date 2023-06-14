package com.rr.utils.cacheClient;

import static com.rr.utils.constants.RedisConstants.CACHE_NULL_TTL;
import static com.rr.utils.constants.RedisConstants.LOCK_SHOP_KEY;

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
  private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

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

  public <R, ID> R queryWithPassThrough(
      String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback,
      Long duration, TimeUnit unit) {
    String key = keyPrefix + id;
    // 1.从redis查询商铺缓存
    String json = get(key);
    // 2.判断是否存在
    if (StrUtil.isNotBlank(json)) {
      // 3.存在，直接返回
      return JSONUtil.toBean(json, type);
    }
    // 判断命中的是否是空值
    if (json != null) {
      // 返回一个错误信息
      return null;
    }
    // 4.不存在，根据id查询数据库,一个HoF的实例
    R r = dbFallback.apply(id);
    // 5.不存在，返回错误
    if (r == null) {
      // 将空值写入redis,防止黑客一直查询数据库造成数据库关闭
      set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
      return null;
    }
    // 6.存在，写入redis
    this.set(key, r, duration, unit);
    return r;
  }

  /**
   * use mutex to  make parallelization  to serialization, also built in with passThrough
   * protection.(set null obj)
   */
  public <R, ID> R queryWithMutex(
      String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long duration,
      TimeUnit unit) {
    String key = keyPrefix + id;
    // 1.从redis查询商铺缓存
    String shopJson = get(key);
    // 2.判断是否存在
    if (StrUtil.isNotBlank(shopJson)) {
      return getBean(type, shopJson);
    }
    // 判断命中的是否是空值
    if (shopJson != null) {
      return null;
    }
    // 4.实现缓存重建
    // 4.1.获取互斥锁
    String lockKey = LOCK_SHOP_KEY + id;
    R r;
    try {
      boolean isLock = tryLock(lockKey);
      // 4.2.判断是否获取成功
      if (!isLock) {
        // 4.3.获取锁失败，休眠并重试
        Thread.sleep(50);
        //goto the beginning to query redis.
        return queryWithMutex(keyPrefix, id, type, dbFallback, duration, unit);
      }
      // 4.4.获取锁成功，根据id查询数据库
      r = dbFallback.apply(id);
      Thread.sleep(
          200);//simulate remote call DB via network.time longer ,need higher reliable of mutex
      // 5.不存在，返回错误
      if (r == null) {
        // 将空值写入redis
        set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
        return null;
      }
      // 6.存在，写入redis
      set(key, r, duration, unit);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      // 7.释放锁
      unlock(lockKey);
    }
    // 8.返回
    return r;
  }

  public <R, ID>
  R queryWithLogicalExpire(
      String keyPrefix, ID id,//id可能是Long或者String都有可能
      Class<R> type, Function<ID, R> dbFallback,
      Long duration, TimeUnit unit) {
    String key = keyPrefix + id;
    // 1.从redis查询缓存
    String json = get(key);
    // 2.判断是否存在
    if (StrUtil.isBlank(json)) {
      // 3.不存在，直接返回
      return null;
    }
    RedisData redisData = deserialize(json);
    R r = deserialize(type, redisData);
    LocalDateTime expireTime = redisData.getExpireTime();
    // 5.判断是否过期
    if (expireTime.isAfter(LocalDateTime.now())) {
      // 5.1.未过期，直接返回店铺信息
      return r;
    }
    // 5.2.已过期，需要尝试缓存重建
    // 6.缓存重建
    // 6.1.获取互斥锁
    String lockKey = LOCK_SHOP_KEY + id;
    boolean isLock = tryLock(lockKey);
    // 6.2.判断是否获取锁成功
    if (isLock) {
      // 6.3.成功，开启独立线程，实现缓存重建
      CACHE_REBUILD_EXECUTOR.submit(() -> {
        try {
          // 查询数据库
          R newR = dbFallback.apply(id);
          // 重建缓存
          setWithLogicalExpire(key, newR, duration, unit);
        } catch (Exception e) {
          throw new RuntimeException(e);
        } finally {
          // 释放锁
          unlock(lockKey);
        }
      });
    }
    // 6.4.未获取到锁,返回过期的商铺信息
    return r;
  }

  /**
   * Redis get
   */
  private String get(String key) {
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
    delete(key);
  }

  private void delete(String key) {
    stringRedisTemplate.delete(key);
  }


  private void set(String key, Object value, Long duration, TimeUnit unit) {
    stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), duration, unit);
  }

  private void setWithLogicalExpire(String key, Object value, Long duration, TimeUnit unit) {
    // 设置逻辑过期
    RedisData redisData = new RedisData();
    redisData.setData(value);
    redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(duration)));
    // 写入Redis
    stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
  }

}
