package com.rr;

import javax.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class Gen100UserWithTokenStoreInDB {

  @Resource
  StringRedisTemplate stringRedisTemplate;

  @Test
  void add(){

  }

}
