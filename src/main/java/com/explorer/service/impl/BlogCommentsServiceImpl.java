package com.explorer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.explorer.entity.BlogComments;
import com.explorer.mapper.BlogCommentsMapper;
import com.explorer.service.IBlogCommentsService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author b
 * @since 2021-12-22
 */
@Service
public class BlogCommentsServiceImpl extends
    ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
