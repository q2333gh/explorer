package com.explorer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.explorer.dto.LoginFormDTO;
import com.explorer.dto.Result;
import com.explorer.entity.User;

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
