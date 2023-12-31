package com.explorer.service.impl;

import static com.explorer.utils.constants.RedisConstants.LOGIN_CODE_KEY;
import static com.explorer.utils.constants.RedisConstants.LOGIN_CODE_TTL;
import static com.explorer.utils.constants.RedisConstants.LOGIN_USER_KEY;
import static com.explorer.utils.constants.RedisConstants.LOGIN_USER_TTL;
import static com.explorer.utils.constants.RedisConstants.USER_SIGN_KEY;
import static com.explorer.utils.constants.SystemConstants.USER_NICK_NAME_PREFIX;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.explorer.dto.LoginFormDTO;
import com.explorer.dto.Result;
import com.explorer.dto.UserDTO;
import com.explorer.entity.User;
import com.explorer.mapper.UserMapper;
import com.explorer.service.IUserService;
import com.explorer.utils.UserHolder;
import com.explorer.utils.regexUtils.RegexUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

  @Resource
  private StringRedisTemplate stringRedisTemplate;

  private static Map<String, Object> trans2Map(User user) {
    UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
    return BeanUtil.beanToMap(userDTO, new HashMap<>(),
        CopyOptions.create()
            .setIgnoreNullValue(true)
            .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
  }

  private static int getCount(Long num) {
    int count = 0;
    while (true) {
      // 6.1.让这个数字与1做与运算，得到数字的最后一个bit位  // 判断这个bit位是否为0
      if ((num & 1) == 0) {
        // 如果为0，说明未签到，结束
        break;
      } else {
        // 如果不为0，说明已签到，计数器+1
        count++;
      }
      // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
      num >>>= 1;
    }
    return count;
  }

  @Override
  public Result sendCode(String phone) {
    // 1.校验手机号
    if (RegexUtils.isPhoneInvalid(phone)) {
      return Result.fail("手机号格式错误！");
    }
    // 3.符合，生成验证码
    String code = RandomUtil.randomNumbers(6);

    // 4.保存验证码到 session
    stringRedisTemplate.opsForValue()
        .set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

    // 5.发送验证码
    log.debug("发送短信验证码成功，验证码：{}", code);

    //    return Result.ok(); // prod
    return Result.ok(code);// debug
  }

  @Override
  public Result login(LoginFormDTO loginForm) {
    String phone = loginForm.getPhone();
    if (RegexUtils.isPhoneInvalid(phone)) {
      return Result.fail("手机号格式错误！");
    }
    // 3.从redis获取验证码并校验
    String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
    String code = loginForm.getCode();
    if (cacheCode == null || !cacheCode.equals(code)) {
      return Result.fail("验证码错误");
    }
    User user = query().eq("phone", phone).one();
    if (user == null) {
      user = createUserWithPhone(phone);
    }
    // 7.1.随机生成token，作为登录令牌
    String token = UUID.randomUUID().toString(true);
    // 7.2.将User对象转为HashMap存储
    Map<String, Object> userMap = trans2Map(user);
    // 7.3.存储
    String tokenKey = LOGIN_USER_KEY + token;
    stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
    // 7.4.设置token有效期
    stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.SECONDS);
    return Result.ok(token);
  }

  @Override
  public Result sign() {
    Long userId = UserHolder.getUser().getId();
    LocalDateTime now = LocalDateTime.now();
    String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
    String key = USER_SIGN_KEY + userId + keySuffix;
    int dayOfMonth = now.getDayOfMonth();
    // 5.写入Redis SETBIT key offset 1
    //    ex: 10011 -> 100111 SETBIT k1 6 1 第六位设置为1
    stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
    return Result.ok();
  }

  @Override
  public Result signCount() {
    // 1.获取当前登录用户
    Long userId = UserHolder.getUser().getId();
    // 2.获取日期
    LocalDateTime now = LocalDateTime.now();
    // 3.拼接key
    String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
    String key = USER_SIGN_KEY + userId + keySuffix;
    // 4.获取今天是本月的第几天
    int dayOfMonth = now.getDayOfMonth();
    // 5.获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字 BITFIELD sign:5:202203 GET u14 0
    List<Long> result = stringRedisTemplate.opsForValue().bitField(
        key,
        BitFieldSubCommands.create()
            .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
    );
    if (result == null || result.isEmpty()) {
      // 没有任何签到结果
      return Result.ok(0);
    }
    Long num = result.get(0);
    if (num == null || num == 0) {
      return Result.ok(0);
    }
    // 6.循环遍历
    int count = getCount(num);
    return Result.ok(count);
  }

  private User createUserWithPhone(String phone) {
    // 1.创建用户
    User user = new User();
    user.setPhone(phone);
    user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
    // 2.保存用户
    save(user);
    return user;
  }
}
