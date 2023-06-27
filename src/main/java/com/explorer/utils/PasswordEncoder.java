package com.explorer.utils;


import cn.hutool.core.util.RandomUtil;
import java.nio.charset.StandardCharsets;
import org.springframework.util.DigestUtils;

public class PasswordEncoder {

  public static String encode(String password) {
    String salt = RandomUtil.randomString(20);    // 生成盐
    return encode(password, salt);    // 加密
  }

  private static String encode(String password, String salt) {
    return salt + "@" + DigestUtils.md5DigestAsHex(    // 加密
        (password + salt).getBytes(StandardCharsets.UTF_8));
  }

  public static Boolean matches(String encodedPassword, String rawPassword) {
    if (encodedPassword == null || rawPassword == null) {
      return false;
    }
    if (!encodedPassword.contains("@")) {
      throw new RuntimeException("密码格式不正确！");
    }
    String[] arr = encodedPassword.split("@");    // 获取盐
    String salt = arr[0];    // 比较
    return encodedPassword.equals(encode(rawPassword, salt));
  }
}
