package com.rr.utils.distributedLock;

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

  static {
    UNLOCK_SCRIPT = new DefaultRedisScript<>();//这里()可以直接硬编码
    UNLOCK_SCRIPT.setLocation(new ClassPathResource("scripts/unlock.lua"));//may cause io exception
    UNLOCK_SCRIPT.setResultType(Long.class);
  }//提前读取脚本到内存.减少IO,
  //    static: 在java启动时就请求操作

  private final String name;
  private final StringRedisTemplate stringRedisTemplate;

  public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
    this.name = name;
    this.stringRedisTemplate = stringRedisTemplate;
  }

  @Override
  public boolean tryLock(long timeoutSec) {
    // 获取线程标示
    String threadId = ID_PREFIX + Thread.currentThread().getId();
    // 获取锁 set lock thread1 NX EX 10
    Boolean success = stringRedisTemplate.opsForValue()
        .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
    return Boolean.TRUE.equals(success);//手动拆箱,suc 返回的是null也能最后返回false
  }

  @Override
  public void unlock() {
    // 调用lua脚本
    stringRedisTemplate.execute(
        UNLOCK_SCRIPT,
        Collections.singletonList(KEY_PREFIX + name),//吧key string放到java-List里面去
        ID_PREFIX + Thread.currentThread().getId());
  }

}
