package com.explorer;

public class dirTest {

  public static void main(String[] args) {
    String dir = System.getProperty("user.dir");
    dir=dir+"./nginx/html/explorer/imgs";
    System.out.println(dir);
  }

}
