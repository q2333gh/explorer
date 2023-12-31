package com.explorer.utils.intercepter;

import static com.explorer.utils.constants.RedisConstants.LOGIN_USER_KEY;
import static com.explorer.utils.constants.RedisConstants.LOGIN_USER_TTL;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.explorer.dto.UserDTO;
import com.explorer.utils.UserHolder;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

public class RefreshTokenInterceptor implements HandlerInterceptor {

  private final StringRedisTemplate stringRedisTemplate;

  public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  private static UserDTO map2bean(Map<Object, Object> userMap) {
    return BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
  }

  @Override
  public boolean preHandle
      (HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String token = request.getHeader("authorization");
    if (StrUtil.isBlank(token)) {
      return true;
    }
    String key = LOGIN_USER_KEY + token;
    Map<Object, Object> userMap = redisGetMap(key);
    if (userMap.isEmpty()) {
      return true;
    }
    UserDTO userDTO = map2bean(userMap);
    UserHolder.saveUser(userDTO);
    redisRefreshTTL(key);
    return true;
  }

  private void redisRefreshTTL(String key) {
    stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES);
  }

  private Map<Object, Object> redisGetMap(String key) {
    return stringRedisTemplate.opsForHash().entries(key);
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) throws Exception {
    UserHolder.removeUser();
  }
}
