package com.rr.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rr.dto.Result;
import com.rr.entity.Follow;

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
