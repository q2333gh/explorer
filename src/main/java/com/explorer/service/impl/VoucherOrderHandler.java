package com.explorer.service.impl;

//import static com.rr.service.impl.VoucherOrderServiceImpl.GetBean;

import static com.explorer.utils.constants.RedisConstants.STREAM_ORDERS;
import static java.lang.Thread.sleep;

import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
public class VoucherOrderHandler implements Runnable {

  @Resource
  private StringRedisTemplate stringRedisTemplate;

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

  private boolean tryOrder() {
    // 1.获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 >
    //count maybe many,so return List ;here only 1
    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
        Consumer.from("g1", "c1"),
        StreamReadOptions.empty().count(1),
        StreamOffset.create(STREAM_ORDERS, ReadOffset.from("0"))
    );
    if (list == null || list.isEmpty()) {
      // 如果为null，说明没有消息，继续下一次循环
      //            Thread.sleep(100);  ->debug use
      //            break ->  debug ;  prod -> continue.
      return true;
      //            continue;
    }
    MapRecord<String, Object, Object> record = list.get(0);
    //    createVoucherOrder(GetBean(record.getValue())); //TODO
    ack(record);
    return false;
  }

  private void ack(MapRecord<String, Object, Object> record) {
    stringRedisTemplate.opsForStream().acknowledge("s1", "g1", record.getId());
  }

  /**
   * 处理取了没消费的未确认订单,确保拿走的订单都要消费
   */
  private void handlePendingList() throws InterruptedException {
    while (true) {
      try {
        // 1.获取pending-list中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 0
        //          0 -> 代表读取PendingList
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
