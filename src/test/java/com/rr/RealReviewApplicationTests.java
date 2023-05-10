package com.rr;

import static com.rr.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.rr.utils.RedisConstants.SHOP_GEO_KEY;

import com.rr.entity.Shop;
import com.rr.service.impl.ShopServiceImpl;
import com.rr.utils.CacheClient;
import com.rr.utils.RedisIdWorker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class RealReviewApplicationTests {

  @Resource
  private CacheClient cacheClient;

  @Resource
  private ShopServiceImpl shopService;

  @Resource
  private RedisIdWorker redisIdWorker;

  @Resource
  private StringRedisTemplate stringRedisTemplate;

//  @Resource
  private final ExecutorService es = Executors.newFixedThreadPool(500);

  @Test
  void testIdWorker() throws InterruptedException {
    AtomicLong idsNum= new AtomicLong(0L);

    int t=300;
    CountDownLatch latch = new CountDownLatch(t);//timer inside each thread.
    Runnable task = () -> {
      for (int i = 0; i < 100; i++) {
        long id = redisIdWorker.nextId("order");
//        System.out.println("id = " + id);
        idsNum.getAndIncrement();
      }
      latch.countDown();
    };
    long begin = System.currentTimeMillis();
    for (int i = 0; i < t; i++) {
      es.submit(task);
    }
    latch.await();
    long end = System.currentTimeMillis();
    long delta=(end - begin);

    System.out.println("time = " + delta+"ms");
    System.out.println("idsNum = "+idsNum);
    double gps=idsNum.doubleValue()/((double) delta/1000);
    System.out.println("generating speed : "+gps+" id/s");
  }

  @Test
  void testSaveShop() throws InterruptedException {
    Shop shop = shopService.getById(1L);
//    cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 1L, shop, 10L, TimeUnit.SECONDS);
  }

  @Test
  void loadShopData() {
    // 1.查询店铺信息
    List<Shop> list = shopService.list();
    // 2.把店铺分组，按照typeId分组，typeId一致的放到一个集合
    Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
    // 3.分批完成写入Redis
    for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
      // 3.1.获取类型id
      Long typeId = entry.getKey();
      String key = SHOP_GEO_KEY + typeId;
      // 3.2.获取同类型的店铺的集合
      List<Shop> value = entry.getValue();
      List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
      // 3.3.写入redis GEOADD key 经度 纬度 member
      for (Shop shop : value) {
        // stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
        locations.add(new RedisGeoCommands.GeoLocation<>(
            shop.getId().toString(),
            new Point(shop.getX(), shop.getY())
        ));
      }
      stringRedisTemplate.opsForGeo().add(key, locations);
    }
  }

  @Test
  void testHyperLogLog() {
    String[] values = new String[1000];
    int j = 0;
    for (int i = 0; i < 1000000; i++) {
      j = i % 1000;
      values[j] = "user_" + i;
      if (j == 999) {
        // 发送到Redis
        stringRedisTemplate.opsForHyperLogLog().add("hl2", values);
      }
    }
    // 统计数量
    Long count = stringRedisTemplate.opsForHyperLogLog().size("hl2");
    System.out.println("count = " + count);
  }

  @Test
  void testThisPointer() {
    Shop shop = new Shop();
    long id = 1;
    //    Result x = cacheClient.queryWithPassThrough("x", id, Shop.class, shopService.queryById(id), 10,
    //        TimeUnit.MINUTES);
  }

  @Test
  void testSecKill(){
    String s = stringRedisTemplate.opsForValue().get("seckill:stock:15");
    System.out.println("aaa"+s);
  }
}
