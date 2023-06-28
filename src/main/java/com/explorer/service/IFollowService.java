package com.explorer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.explorer.dto.Result;
import com.explorer.entity.Follow;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author b
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

  Result follow(Long followUserId, Boolean isFollow);

  Result isFollow(Long followUserId);

  Result followCommons(Long id);
}
