package com.rr.service.impl;

import static com.rr.utils.constants.RedisConstants.SECKILL_STOCK_KEY;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rr.dto.Result;
import com.rr.entity.SeckillVoucher;
import com.rr.entity.Voucher;
import com.rr.mapper.VoucherMapper;
import com.rr.service.ISeckillVoucherService;
import com.rr.service.IVoucherService;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author b
 * @since 2021-12-22
 */
@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements
    IVoucherService {

  @Resource
  private ISeckillVoucherService seckillVoucherService;
  @Resource
  private StringRedisTemplate stringRedisTemplate;

  @Override
  public Result queryVoucherOfShop(Long shopId) {
    // 查询优惠券信息
    List<Voucher> vouchers = getBaseMapper().queryVoucherOfShop(shopId);
    // 返回结果
    return Result.ok(vouchers);
  }

  @Override
  @Transactional
  public void addSeckillVoucher(Voucher voucher) {
    save(voucher);
    save2Seckill(voucher);
    cache2Redis(voucher);
  }

  private void save2Seckill(Voucher voucher) {
    SeckillVoucher seckillVoucher = new SeckillVoucher();
    seckillVoucher.setVoucherId(voucher.getId());
    seckillVoucher.setStock(voucher.getStock());
    seckillVoucher.setBeginTime(voucher.getBeginTime());
    seckillVoucher.setEndTime(voucher.getEndTime());
    seckillVoucherService.save(seckillVoucher);
  }

  private void cache2Redis(Voucher voucher) {
    stringRedisTemplate.opsForValue()
        .set(SECKILL_STOCK_KEY + voucher.getId(), voucher.getStock().toString());
  }
}
