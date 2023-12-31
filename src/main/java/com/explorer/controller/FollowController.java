package com.explorer.controller;


import com.explorer.dto.Result;
import com.explorer.service.IFollowService;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author b
 */
@RestController
@RequestMapping("/follow")
public class FollowController {

  @Resource
  private IFollowService followService;

  @PutMapping("/{id}/{isFollow}")
  public Result follow(@PathVariable("id") Long followUserId,
      @PathVariable("isFollow") Boolean isFollow) {
    return followService.follow(followUserId, isFollow);
  }

  @GetMapping("/or/not/{id}")
  public Result isFollow(@PathVariable("id") Long followUserId) {
    return followService.isFollow(followUserId);
  }

  @GetMapping("/common/{id}")
  public Result followCommons(@PathVariable("id") Long id) {
    return followService.followCommons(id);
  }
}
