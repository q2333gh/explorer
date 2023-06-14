package com.rr.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rr.entity.ShopType;
import com.rr.mapper.ShopTypeMapper;
import com.rr.service.IShopTypeService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author b
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements
    IShopTypeService {

}
