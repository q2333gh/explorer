package com.explorer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.explorer.entity.Voucher;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author b
 * @since 2021-12-22
 */
public interface VoucherMapper extends BaseMapper<Voucher> {

  List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}
