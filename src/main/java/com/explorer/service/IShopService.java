package com.explorer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.explorer.dto.Result;
import com.explorer.entity.Shop;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author b
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

  Result queryById(Long id);

  Result update(Shop shop);

  Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
