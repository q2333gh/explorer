package com.rr.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rr.dto.Result;
import com.rr.entity.Voucher;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author b
 * @since 2021-12-22
 */
public interface IVoucherService extends IService<Voucher> {

  Result queryVoucherOfShop(Long shopId);

  void addSeckillVoucher(Voucher voucher);
}
