package com.explorer;

import com.explorer.dto.Result;
import com.explorer.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
public class AOP_test {

  @Mock
  private StringRedisTemplate stringRedisTemplate;

  @InjectMocks
  private UserServiceImpl myService;

  @Test
  public void testSendCode() {
    String phone = "1234567890";
    Result result = myService.sendCode(phone);

    System.out.println(result);

  }


  /*
  Introspection in Java ,
  means , how a user class get it`s metainfo about
  how it`s structure is and tell it to some other program.

  application : Remote Procedure Call? is it use ?

   */
  @Test
  public void testInvoke() {
    Class<Result> resultClass = Result.class;
    System.out.println(resultClass);

  }

}
