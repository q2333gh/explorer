package com.rr.config;

import com.rr.utils.intercepter.LoginInterceptor;
import com.rr.utils.intercepter.RefreshTokenInterceptor;
import javax.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

  @Resource
  private StringRedisTemplate stringRedisTemplate;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // token刷新的拦截器
    registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
        .addPathPatterns("/**")
        .order(0);
    // 登录拦截器
    registry.addInterceptor(new LoginInterceptor())
        .excludePathPatterns(
            "/**",  // this line  for test
            "/shop/**",
            "/voucher/**",
            "/shop-type/**",
            "/upload/**",
            "/blog/hot",
            "/user/code",
            "/user/login"
        )
        .order(1);
  }
}
