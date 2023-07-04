//package com.explorer.config;
//
//import org.redisson.Redisson;
//import org.redisson.api.RedissonClient;
//import org.redisson.config.Config;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.env.Environment;
//
//@Configuration
//public class RedissonConfig_bkup {
//
//  final
//  Environment env;
//
//  public RedissonConfig_bkup(Environment env) {
//    this.env = env;
//  }
//
//  @Bean
//  public RedissonClient redissonClient() {
//    Config config = new Config();
//    config.useSingleServer()
//        .setAddress(env.getProperty("RedisSingleServerConfig.address"))//从yaml 文件里面读的
//        .setPassword(env.getProperty("RedisSingleServerConfig.password"));
//    return Redisson.create(config);
//  }
//}
