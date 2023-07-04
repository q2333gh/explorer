package com.explorer.utils.constants;


public class SystemConstants {

  //    注意图片地址
//  这应该是运行时获取的,更灵活一些. System.getProperty("user.dir")
//  上面是获取java线程运行时的路径.
  public static final String IMAGE_UPLOAD_DIR =
//      "./nginx/html/explorer/imgs";
      "T:\\1_code\\nginx-1.18.0-rr\\html\\hmdp\\imgs\\";
  public static final String USER_NICK_NAME_PREFIX = "user_";
  public static final int DEFAULT_PAGE_SIZE = 5;
  public static final int MAX_PAGE_SIZE = 10;
}
