package com.explorer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.explorer.mapper")
@SpringBootApplication
public class ExplorerApplication {
  public static void main(String[] args) {
    SpringApplication.run(ExplorerApplication.class, args);
  }
}
