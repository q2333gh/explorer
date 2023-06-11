package com.rr.controller;


import com.rr.dto.Result;
import com.rr.entity.Voucher;
import com.rr.service.IVoucherService;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
@RequestMapping("/voucher")
public class VoucherController {

  @Resource
  private IVoucherService voucherService;

  /**
   * 新增秒杀券
   *
   * @param voucher 优惠券信息，包含秒杀信息
   * @return 优惠券id
   */
  @PostMapping("add_seckill")
  public Result addSeckillVoucher(@RequestBody Voucher voucher) {
    voucherService.addSeckillVoucher(voucher);
    return Result.ok("vid:" + voucher.getId());
  }

  /**
   * 新增普通券
   *
   * @param voucher 优惠券信息
   * @return 优惠券id
   */
  @PostMapping("add_normal")
  public Result addVoucher(@RequestBody Voucher voucher) {
    voucherService.save(voucher);
    return Result.ok(voucher.getId());
  }


  /**
   * 查询店铺的优惠券列表
   *
   * @param shopId 店铺id
   * @return 优惠券列表
   */
  @GetMapping("/list/{shopId}")
  public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
    return voucherService.queryVoucherOfShop(shopId);
  }

}
