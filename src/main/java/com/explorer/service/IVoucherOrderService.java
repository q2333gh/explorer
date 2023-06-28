package com.explorer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.explorer.dto.Result;
import com.explorer.entity.VoucherOrder;

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
