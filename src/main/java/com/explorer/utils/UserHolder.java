package com.explorer.utils;

import com.explorer.dto.UserDTO;
/**
 * ThreaLocal Hold Data
 */
public class UserHolder {

  private static final ThreadLocal<UserDTO> tl = new ThreadLocal<>();

  public static void saveUser(UserDTO user) {
    tl.set(user);
  }

  /**
   * get from threadlocal
   */
  public static UserDTO getUser() {
    return tl.get();
  }

  public static void removeUser() {
    tl.remove();
  }
}
