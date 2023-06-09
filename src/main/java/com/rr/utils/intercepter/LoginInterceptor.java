package com.rr.utils.intercepter;

import com.rr.utils.UserHolder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (UserHolder.getUser() == null) {
      response.setStatus(401);
      return false;
    }
    return true;
  }
}
