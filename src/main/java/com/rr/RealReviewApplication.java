package com.rr;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.rr.mapper")
@SpringBootApplication
public class RealReviewApplication {

  public static void main(String[] args) {
    SpringApplication.run(RealReviewApplication.class, args);
  }

}
