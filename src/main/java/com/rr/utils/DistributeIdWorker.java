package com.rr.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 一共64bit信息, 第1位符号位, 余下31位时间戳 再余下32位序列号 序列号由keyPrefix+当前日期组合而成
 */
@Component
public class DistributeIdWorker {

  /**
   * 开始时间戳
   */
  private static final long BEGIN_TIMESTAMP = 1640995200L;
  //  如:1970.1.1到2022.1.1的秒数差距 :
  //  LocalDateTime.of(2022,1,1,0,0,0).toEpochSecond(ZoneOffset.UTC)

  /**
   * 序列号的位数
   */
  private static final int COUNT_BITS = 32;

  private final StringRedisTemplate stringRedisTemplate;

  public DistributeIdWorker(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  private static long getInterval(LocalDateTime now) {
    long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
    return nowSecond - BEGIN_TIMESTAMP;
  }

  /**
   * 把timestamp左移了32位,再异或运算,在这里需要:00->0 ,01->1,即拼接
   */
  private static long cat(long interval, long serial) {
    return interval << COUNT_BITS | serial;
  }

  private static String getDate(LocalDateTime now) {
    return now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
  }

  public long nextId(String keyPrefix) {
    // 1.生成时间戳
    LocalDateTime now = LocalDateTime.now();
    long interval = getInterval(now);
    // 2.1.获取当前日期字符串，精确到天: 1.每天key容量几十亿 2.方便统计筛选 如: date = 2023:02:05
    String date = getDate(now);
    // 2.2.Redis 自增长
    long serial = incr(keyPrefix, date);
    return cat(interval, serial);
  }

  private long incr(String keyPrefix, String date) {
    Long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
    if (increment != null) {
      return increment;
    }
    return -1;
  }
}
