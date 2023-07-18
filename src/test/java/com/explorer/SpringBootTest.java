package com.explorer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

@org.springframework.boot.test.context.SpringBootTest
public class SpringBootTest {

  @Resource
  Environment env;

  @Test
  void testGetYamlConfigIntoJava() {
    System.out.println("aaaaaa");//to anchor the output place. hah , noob but useful
    System.out.println(env.getProperty("redis://" + "spring.redis.host"));
    String pswd = env.getProperty("spring.redis.password");
    System.out.println(pswd);
    assertEquals("123456", pswd);
  }
}
