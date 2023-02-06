package com.rr.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rr.entity.SeckillVoucher;
import com.rr.mapper.SeckillVoucherMapper;
import com.rr.service.ISeckillVoucherService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 服务实现类
 * </p>
 *
 * @author b
 * @since 2022-01-04
 */
@Service
public class SeckillVoucherServiceImpl extends
    ServiceImpl<SeckillVoucherMapper, SeckillVoucher> implements ISeckillVoucherService {

}
