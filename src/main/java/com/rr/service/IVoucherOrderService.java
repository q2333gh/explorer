package com.rr.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rr.dto.Result;
import com.rr.entity.VoucherOrder;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author b
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

  Result seckillVoucher(Long voucherId);
}
