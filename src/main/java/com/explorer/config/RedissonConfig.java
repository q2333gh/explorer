package com.explorer.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class RedissonConfig {

  final
  Environment env;

  public RedissonConfig(Environment env) {
    this.env = env;
  }

  @Bean
  public RedissonClient redissonClient() {
    Config config = new Config();
    String addr = "redis://"//从yaml 文件里面读
        + env.getProperty("spring.redis.host")
        + ":" + env.getProperty("spring.redis.port");
    String pswd = env.getProperty("spring.redis.password");
    config.useSingleServer()
        .setAddress(addr)
        .setPassword(pswd);
    return Redisson.create(config);
  }
}
