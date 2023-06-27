package com.explorer.controller;


import com.explorer.dto.Result;
import com.explorer.entity.ShopType;
import com.explorer.service.IShopTypeService;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/shop-type")
public class ShopTypeController {

  @Resource
  private IShopTypeService typeService;

  @GetMapping("list")
  public Result queryTypeList() {
    List<ShopType> typeList = typeService
        .query().orderByAsc("sort").list();
    return Result.ok(typeList);
  }
}
