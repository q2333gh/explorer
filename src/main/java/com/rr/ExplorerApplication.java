package com.rr;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.rr.mapper")
@SpringBootApplication
public class ExplorerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ExplorerApplication.class, args);
  }

}
