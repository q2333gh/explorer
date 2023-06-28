package com.explorer.controller;


import com.explorer.dto.Result;
import com.explorer.service.IVoucherOrderService;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequestMapping("/voucher-order")
public class VoucherOrderController {

  @Resource
  private IVoucherOrderService voucherOrderService;

  @PostMapping("seckill/{id}")
  public Result seckillVoucher(@PathVariable("id") Long voucherId) {
    return voucherOrderService.seckillVoucher(voucherId);
  }
}
