package com.rr.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rr.entity.UserInfo;
import com.rr.mapper.UserInfoMapper;
import com.rr.service.IUserInfoService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author b
 * @since 2021-12-24
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements
    IUserInfoService {

}
