package com.rr.service.impl;

import static com.rr.utils.constants.RedisConstants.CACHE_SHOP_KEY;
import static com.rr.utils.constants.RedisConstants.CACHE_SHOP_TTL;
import static com.rr.utils.constants.RedisConstants.SHOP_GEO_KEY;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rr.dto.Result;
import com.rr.entity.Shop;
import com.rr.mapper.ShopMapper;
import com.rr.service.IShopService;
import com.rr.utils.redisClient.CacheClient;
import com.rr.utils.constants.SystemConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {


  @Resource
  private StringRedisTemplate stringRedisTemplate;

  @Resource
  private CacheClient cacheClient;

  @Override
  public Result queryById(Long id) {
    // 解决缓存穿透
    //    Shop shop = cacheClient
    //        .queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL,
    //            TimeUnit.MINUTES);

    //     互斥锁解决缓存击穿
    Shop shop = cacheClient
        .queryWithMutex(
            CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

    // 逻辑过期解决缓存击穿
    // Shop shop = cacheClient
    //         .queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);

    if (shop == null) {
      return Result.fail("店铺不存在！");
    }
    // 7.返回
    return Result.ok(shop);
  }

  @Override
  @Transactional
  public Result update(Shop shop) {
    Long id = shop.getId();
    if (id == null) {
      return Result.fail("店铺id不能为空");
    }
    updateById(shop);    // 1.更新数据库
    stringRedisTemplate.delete(CACHE_SHOP_KEY + id);    // 2.删除缓存
    return Result.ok();
  }

  @Override
  public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
    if (x == null || y == null) {    // 1.判断是否需要根据坐标查询
      Page<Shop> page = query()      // 不需要坐标查询，按数据库查询
          .eq("type_id", typeId)
          .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
      return Result.ok(page.getRecords());      // 返回数据
    }

    int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;    // 2.计算分页参数
    int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

    String key = SHOP_GEO_KEY + typeId;    // 3.查询redis、按照距离排序、分页。结果：shopId、distance
    GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo() // GEOSEARCH key BYLONLAT x y BYRADIUS 10 WITHDISTANCE
        .search(
            key,
            GeoReference.fromCoordinate(x, y),
            new Distance(5000),
            RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
        );

    if (results == null) {    // 4.解析出id
      return Result.ok(Collections.emptyList());
    }
    List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
    if (list.size() <= from) {
      return Result.ok(Collections.emptyList());      // 没有下一页了，结束
    }
    List<Long> ids = new ArrayList<>(list.size());    // 4.1.截取 from ~ end的部分
    Map<String, Distance> distanceMap = new HashMap<>(list.size());
    list.stream().skip(from).forEach(result -> {
      String shopIdStr = result.getContent().getName();      // 4.2.获取店铺id
      ids.add(Long.valueOf(shopIdStr));
      Distance distance = result.getDistance();      // 4.3.获取距离
      distanceMap.put(shopIdStr, distance);
    });
    String idStr = StrUtil.join(",", ids);    // 5.根据id查询Shop
    List<Shop> shops = query()
        .in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
    for (Shop shop : shops) {
      shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
    }
    return Result.ok(shops);
  }
}
