package com.hmdp.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisIdWorker {

  /**
   * 开始时间戳
   */
  private static final long BEGIN_TIMESTAMP = 1640995200L;
  //  如:1970.1.1到2022.1.1的秒数差距 : LocalDateTime.of(2022,1,1,0,0,0).toEpochSecond(ZoneOffset.UTC)
  /**
   * 序列号的位数
   */
  private static final int COUNT_BITS = 32;

  private final StringRedisTemplate stringRedisTemplate;

  public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  public long nextId(String keyPrefix) {
    // 1.生成时间戳
    LocalDateTime now = LocalDateTime.now();
    long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
    long timestamp = nowSecond - BEGIN_TIMESTAMP;

    // 2.生成序列号
    // 2.1.获取当前日期，精确到天: 1.每天key容量几十亿 2.方便统计筛选
    String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
    // 2.2.自增长
    long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

    // 3.拼接并返回
    return timestamp << COUNT_BITS | count;
  }
}
