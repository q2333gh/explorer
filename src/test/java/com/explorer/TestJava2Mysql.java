package com.explorer;


import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestJava2Mysql {


  @Value("${testdata}")
  public String td;

  @Test
  public void run() {
    System.out.println("hello, world!");

    System.out.println("555"+td);
  }
}
