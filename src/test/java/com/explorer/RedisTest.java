package com.explorer;

import static com.explorer.utils.constants.RedisConstants.CONSUMER_GROUP;
import static com.explorer.utils.constants.RedisConstants.CONSUMER_NAME;
import static com.explorer.utils.constants.RedisConstants.STREAM_ORDERS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import javax.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class RedisTest {

  @Resource
  StringRedisTemplate stringRedisTemplate;

  @Test
  void connect() {
    stringRedisTemplate.opsForValue().set("k11", "v11");
    String v = stringRedisTemplate.opsForValue().get("k11");
    assertEquals("v11", v);
  }

  @Test
  void testStream() {
    List<MapRecord<String, Object, Object>> read = stringRedisTemplate.opsForStream().read(
        Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
        StreamReadOptions
            .empty()
            .count(1)
            .block(Duration.ofSeconds(2)),
        StreamOffset.create(
            STREAM_ORDERS, ReadOffset.lastConsumed()
        ));
    System.out.println(read);
  }
}
