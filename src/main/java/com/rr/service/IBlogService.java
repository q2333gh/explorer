package com.rr.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rr.dto.Result;
import com.rr.entity.Blog;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author b
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

  Result queryHotBlog(Integer current);

  Result queryBlogById(Long id);

  Result likeBlog(Long id);

  Result queryBlogLikes(Long id);

  Result saveBlog(Blog blog);

  Result queryBlogOfFollow(Long max, Integer offset);

}
