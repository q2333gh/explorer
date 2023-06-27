package com.explorer.utils.redisClient.distributedLock;

import cn.hutool.core.lang.UUID;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public class SimpleRedisLock implements ILock {

  private static final String KEY_PREFIX = "lock:";

  //use for thread; final : once give val.can`t change in each thread.
  private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
  private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

  static {  //    static: 在java启动时就请求操作
    UNLOCK_SCRIPT = new DefaultRedisScript<>();//这里()可以直接硬编码
    UNLOCK_SCRIPT.setLocation(new ClassPathResource("scripts/unlock.lua"));//may cause io exception
    UNLOCK_SCRIPT.setResultType(Long.class);
  }//提前读取脚本到内存.减少IO,


  private final String name;
  private final StringRedisTemplate stringRedisTemplate;

  public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
    this.name = name;
    this.stringRedisTemplate = stringRedisTemplate;
  }

  @Override
  public boolean tryLock(long timeoutSec) {
    String threadId = ID_PREFIX + Thread.currentThread().getId();    // 获取线程标示
    Boolean success = stringRedisTemplate.opsForValue()    // 获取锁 set lock thread1 NX EX 10
        .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
    return Boolean.TRUE.equals(success);//手动拆箱,suc 返回的是null也能最后返回false
  }

  @Override
  public void unlock() {
    stringRedisTemplate.execute(    // 调用lua脚本
        UNLOCK_SCRIPT,
        Collections.singletonList(KEY_PREFIX + name),//吧key string放到java-List里面去
        ID_PREFIX + Thread.currentThread().getId());
  }

}
