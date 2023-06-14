package com.rr.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rr.entity.BlogComments;
import com.rr.mapper.BlogCommentsMapper;
import com.rr.service.IBlogCommentsService;
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
