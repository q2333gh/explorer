package com.rr.service.impl;

import static com.rr.utils.constants.RedisConstants.LOCK_ORDER;
import static com.rr.utils.constants.RedisConstants.SECKILL_STOCK_KEY;
import static com.rr.utils.constants.RedisConstants.STREAM_ORDERS;
import static java.lang.Thread.sleep;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rr.dto.Result;
import com.rr.entity.VoucherOrder;
import com.rr.mapper.VoucherOrderMapper;
import com.rr.service.ISeckillVoucherService;
import com.rr.service.IVoucherOrderService;
import com.rr.utils.DistributeIdWorker;
import com.rr.utils.UserHolder;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends
    ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
  private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
  /**
  ThreadPool
   */
  private static final ExecutorService SECKILL_ORDER_EXECUTOR =
      Executors.newSingleThreadExecutor();

  private enum OrderStatus {
    SUCCESS(0),
    INSUFFICIENT_INVENTORY(1),
    DUPLICATE_ORDER(2);
    private final int code;
    OrderStatus(int code) {
      this.code = code;
    }
    public int getCode() {
      return code;
    }
  }

  static {
    SECKILL_SCRIPT = new DefaultRedisScript<>();
    SECKILL_SCRIPT.setLocation(new ClassPathResource("scripts/seckill.lua"));
    SECKILL_SCRIPT.setResultType(Long.class);
  }

  @Resource
  private ISeckillVoucherService seckillVoucherService;
  @Resource
  private DistributeIdWorker distributeIdWorker;
  @Resource
  private RedissonClient redissonClient;
  @Resource
  private StringRedisTemplate stringRedisTemplate;

  /**
   * 这个类是常驻消费者线程，持续监听 Redis Stream 消息队列，
   * 而如果我们没有创建队列的话,就会一直抛异常。
   * 先在redis里面创建MQ 再启动Java,创建MQ的命令: XGROUP CREATE stream.orders g1 0 MKSTREAM
   */
  @PostConstruct//借助Spring.在当前类初始化完毕立即执行下列函数,AOP的一个应用
  private void init() {
    SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
  }

  @Override
  public Result seckillVoucher(Long voucherId) {
    if (!Exist(voucherId)) { return Result.fail("voucher not exist!"); }
    Long userId = UserHolder.getUser().getId();
    long orderId = distributeIdWorker.nextId("order");

    Long result = execSeckill(voucherId, userId, orderId);

    return decideRet(orderId, result);
  }

  private static Result decideRet(long orderId, Long result) {
    if (result == null) {
      return Result.fail("query failed");
    }
    int res = result.intValue();//long -> int ;narrow convert;truncation
    if (res != Success()) {
      return Result.fail(res == Insufficient() ?
          "inventory insufficient" : "not allow duplicate order");
    }
    return Result.ok(orderId);
  }

  private Long execSeckill(Long voucherId, Long userId, long orderId) {
    return stringRedisTemplate.execute(
        SECKILL_SCRIPT,
        Collections.emptyList(),//give empty list instead of null
        voucherId.toString(), userId.toString(), String.valueOf(orderId)
    );
  }

  private void createVoucherOrder(VoucherOrder voucherOrder) {
    Long userId = voucherOrder.getUserId();
    Long voucherId = voucherOrder.getVoucherId();
    RLock redisLock = redissonClient.getLock(LOCK_ORDER + userId);
    boolean isLock = redisLock.tryLock();
    if (!isLock) {
      log.error(notAllowDuplicate());
      return;
    }
    try {
      int count = isDuplicated(userId, voucherId);
      if (count > 0) {
        log.error(notAllowDuplicate());
        return;
      }
      boolean success = submit(voucherId);
      if (!success) {
        log.error(insufficient());
        return;
      }
      save(voucherOrder);
    } finally {
      redisLock.unlock();
    }
  }

  private static String insufficient() {
    return "库存不足！";
  }

  private static String notAllowDuplicate() {
    return "不允许重复下单！";
  }

  private boolean submit(Long voucherId) {
    return seckillVoucherService.update()
        .setSql("stock = stock - 1")
        .eq("voucher_id", voucherId).gt("stock", 0)
        .update();
  }

  private Integer isDuplicated(Long userId, Long voucherId) {
    return query().eq("user_id", userId).eq("voucher_id", voucherId).count();
  }

  private class VoucherOrderHandler implements Runnable {
    @Override
    public void run() {
      while (true) {
        try {
          if (tryOrder()) {
            break;
          }
        } catch (Exception e) {
          log.error("处理订单异常", e);
          try {
            handlePendingList();
          } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
          }
        }
      }
    }

    /**
     * @return true->consume 1  false->empty
     */
    @Transactional
    private boolean tryOrder() {
      List<MapRecord<String, Object, Object>> list = streamDequeue();      //count maybe many,so return List ;here only 1
      if (list == null || list.isEmpty()) {
        // 如果为null，说明没有消息，继续下一次循环
        //            Thread.sleep(100);   // ->debug use
        return true;
      }
      MapRecord<String, Object, Object> record = list.get(0);
      createVoucherOrder(GetBean(record.getValue()));
      streamAck(record);
      return false;
    }

    /**
     * 获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 >
     */
    @SuppressWarnings("unchecked")
    private List<MapRecord<String, Object, Object>> streamDequeue() {
      return stringRedisTemplate.opsForStream().read(
          Consumer.from("g1", "c1"),
          StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
          StreamOffset.create(STREAM_ORDERS, ReadOffset.from("0"))
      );
    }

    private void streamAck(MapRecord<String, Object, Object> record) {
      stringRedisTemplate.opsForStream().acknowledge("s1", "g1", record.getId());
    }

    /**
     * 处理取了没消费的未确认订单,确保拿走的订单都要消费
     */
    @SuppressWarnings("all")
    private void handlePendingList() throws InterruptedException {
      while (true) {
        try {
          if (tryOrder()) {
            break;
          }
        } catch (Exception e) {
          log.error("处理订单异常", e);
          sleep(20);
          //无需递归调用自己了,会回到取主消息队列的位置
        }
      }
    }
  }

  private static VoucherOrder GetBean(Map<Object, Object> value) {
    return BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
  }
  private static int Insufficient() {
    return OrderStatus.INSUFFICIENT_INVENTORY.getCode();
  }

  private static int Success() {
    return OrderStatus.SUCCESS.getCode();
  }

  private boolean Exist(Long voucherId) {
    String s = stringRedisTemplate.opsForValue().get(SECKILL_STOCK_KEY + voucherId);
    return s != null;
  }

}
