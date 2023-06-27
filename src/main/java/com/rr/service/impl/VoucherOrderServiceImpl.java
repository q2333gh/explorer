package com.rr.service.impl;

//import redis.clients.jedis.PendingArgs;

import static com.rr.utils.constants.RedisConstants.CONSUMER_GROUP;
import static com.rr.utils.constants.RedisConstants.CONSUMER_NAME;
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
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends
    ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

  /**
   * newSingleThreadExecutor Concepts:
   * <p>1.setting the keep-alive time to 0 in a ThreadPoolExecutor will not
   * cause the thread to rapidly   restart. If the keep-alive time is set to 0, it means that idle
   * threads will not be terminated due to inactivity. for ex: KAT=30s -> 30s  idle thread
   * terminated ; 0 -> not terminate</p>
   * <p>2.performance view : terminate or restart is fully equal to one static thread.
   * but : ExecutorService provide additional benefits: modularity , maintainability, and easy to
   * use.</p>
   * <p>3.design pattern: factory mode : provides an interface for creating objects,
   * but allows subclasses to alter the type of objects</p>
   */
  private static final ExecutorService SECKILL_ORDER_EXECUTOR =
      Executors.newSingleThreadExecutor();

  private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

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

  private static Result decideRet(long orderId, Long result) {
    if (result == null) {
      return Result.fail("query failed");
    }
    int res = result.intValue();//long -> int ;narrow convert;truncation
    if (res != Success()) {
      return Result.fail(res == Insufficient() ?
          "inventory insufficient" : "not allow duplicate order");
    }
    return Result.ok("orderId: " + orderId);
  }

  private static String insufficient() {
    return "库存不足！";
  }

  private static String notAllowDuplicate() {
    return "不允许重复下单！";
  }

  private static VoucherOrder map2Bean(Map<Object, Object> value) {
    return BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
  }

  private static int Insufficient() {
    return OrderStatus.INSUFFICIENT_INVENTORY.getCode();
  }

  private static int Success() {
    return OrderStatus.SUCCESS.getCode();
  }

  @Override
  public Result seckillVoucher(Long voucherId) {
    if (!Exist(voucherId)) {
      return Result.fail("voucher not exist!");
    }
    Long userId = UserHolder.getUser().getId();
    long orderId = distributeIdWorker.nextId("order");

    Long result = execSeckill(voucherId, userId, orderId);

    return decideRet(orderId, result);
  }

  /**
   * 常驻消费者线程，持续监听 Redis Stream 消息队列
   */
  @PostConstruct//借助Spring.在当前类初始化完毕立即执行下列函数,AOP的一个应用
  private void init() {
    SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
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
      boolean success = stock_minus_1(voucherId);
      if (!success) {
        log.error(insufficient());
        return;
      }
      save(voucherOrder);//DB SQL
    } finally {
      redisLock.unlock();
    }
  }

  private boolean stock_minus_1(Long voucherId) {
    return seckillVoucherService.update()
        .setSql("stock = stock - 1")
        .eq("voucher_id", voucherId).gt("stock", 0)
        .update();
  }

  private Integer isDuplicated(Long userId, Long voucherId) {
    return Math.toIntExact(query().eq("user_id", userId).eq("voucher_id", voucherId).count());
  }

  private boolean Exist(Long voucherId) {
    String s = stringRedisTemplate.opsForValue().get(SECKILL_STOCK_KEY + voucherId);
    return s != null;
  }

  /**
   * 处理取了没消费的未确认订单,确保拿走的订单都要消费
   */
  @SuppressWarnings("all")
  private void handlePendingList() throws InterruptedException {
    while (true) {
      try {
        if (consumePendingList()) {
          break;
        }
      } catch (Exception e) {
        log.error("pengdingst 处理异常", e);
        sleep(20);  //无需递归调用自己了,会回到取主消息队列的位置
      }
    }
  }

  private void tryConsumeMainStream() {
    List<MapRecord<String, Object, Object>> list = streamDequeue();      //count maybe many,so return List ;here only 1
    if (list == null || list.isEmpty()) {
      return;        // 如果为null，说明没有消息，继续下一次循环
    }
    MapRecord<String, Object, Object> record = list.get(0);
    createVoucherOrder(map2Bean(record.getValue()));
    streamAck(record.getId());
  }

  private boolean consumePendingList() {
    List<MapRecord<String, Object, Object>> list = pendingListDequeue();      //count maybe many,so return List ;here only 1
    if (list == null || list.isEmpty()) {
      return true;  // 如果为null，说明没有消息，结束读取pendingList.回到主stream循环
    }
    MapRecord<String, Object, Object> record = list.get(0);
    createVoucherOrder(map2Bean(record.getValue()));
    streamAck(record.getId());
    return false;
  }

  /**
   * 获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 >
   */
  @SuppressWarnings("unchecked")
  private List<MapRecord<String, Object, Object>> streamDequeue() {
    return stringRedisTemplate.opsForStream().read(
        Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
        StreamReadOptions
            .empty()
            .count(1)
            .block(Duration.ofSeconds(2)),
        StreamOffset.create(
            STREAM_ORDERS, ReadOffset.lastConsumed()
        )
        //        .block(Duration.ofSeconds(2)) explain :
        //        2 case:
        //          queue empty -> redis thread block 2s. and try next time (outside infi-loop)
        //              java internal timer signal.
        //          queue have element(s) ->  dequeue now. and keep next dequeue.
        //              will not block 2s. but unblock until the redis-remote signal.
    );
  }

  /**
   * PendingList中的消息,一次读一条 XPENDING  stream.orders  g1 - + 1
   */
  @SuppressWarnings("unchecked")
  private List<MapRecord<String, Object, Object>> pendingListDequeue() {
    return stringRedisTemplate.opsForStream().read(
        Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
        StreamReadOptions
            .empty()
            .count(1)
            .block(Duration.ofSeconds(2)),
        StreamOffset.create(
            STREAM_ORDERS, ReadOffset.from("0")
        )
        //        .block(Duration.ofSeconds(2)) explain :
        //        2 case:
        //          queue empty -> redis thread block 2s. and try next time (outside infi-loop)
        //              java internal timer signal.
        //          queue have element(s) ->  dequeue now. and keep next dequeue.
        //              will not block 2s. but unblock until the redis-remote signal.
    );
  }

  /**
   * ACK 以后并不会真正删除这条消息.而是把指针放到其他位置去 由RESP app或者XLEN命令可以检查到. 一般使用2个指针,一个是 last consumed
   * 另一个是pendingList中的 0
   */
  private void streamAck(RecordId id) {
    Long ack = stringRedisTemplate.opsForStream()
        .acknowledge(STREAM_ORDERS, CONSUMER_GROUP, id);
    if (ack == null || ack.intValue() != 1) {
      log.debug("redis stream ack error!");
    }
  }

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

  private class VoucherOrderHandler implements Runnable {

    boolean running = true;

    @Override
    public void run() {
      while (running) {//stream 内部设置了2秒阻塞等待.不会高cpu空转
        try {
          tryConsumeMainStream();
        } catch (Exception e) {
          log.error("处理订单异常,尝试从stream第一条消息消费", e);
          try {
            handlePendingList();
          } catch (InterruptedException ex) {
            running = false;// handlePendingList()  got a try catch. won`t run to here.
            //code semantics aim to  always keep reading the stream. means an infi-loop.
          }
        }
      }
    }
  }

}


