package com.rr.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rr.dto.LoginFormDTO;
import com.rr.dto.Result;
import com.rr.entity.User;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author b
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

  Result sendCode(String phone);

  Result login(LoginFormDTO loginForm);

  Result sign();

  Result signCount();

}
