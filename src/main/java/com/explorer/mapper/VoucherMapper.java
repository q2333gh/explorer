package com.explorer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.explorer.entity.Voucher;
import java.util.List;
import org.apache.ibatis.annotations.Param;


public interface VoucherMapper extends BaseMapper<Voucher> {

  List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
