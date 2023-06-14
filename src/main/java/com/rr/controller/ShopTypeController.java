package com.rr.controller;


import com.rr.dto.Result;
import com.rr.entity.ShopType;
import com.rr.service.IShopTypeService;
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
