--prereq: 确保消息队列存在:
--127.0.0.1:6379> XGROUP CREATE stream.orders g1 0 MKSTREAM
--整个脚本具有Redis原子性

--         >>> para list
local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

--         >>> return:
--  1 -> 库存不足
--  2 -> 重复下单
--  0 -> 下单成功

--          >>> 准备数据
local stockKey = 'seckill:stock:' .. voucherId -- Lua syntax: 字符串拼接用 ..
local orderKey = 'seckill:order:' .. voucherId

if (tonumber(redis.call('get', stockKey)) <= 0) then
  --判断库存是否充足,Redis取出的是string,转number进行0比对,底层使用Set数据结构
  return 1    -- 库存不足
end
if (redis.call('sismember', orderKey, userId) == 1) then
  --3.2.判断用户是否下单 SISMEMBER orderKey userId
  return 2     -- 重复下单
end
redis.call('incrby', stockKey, -1)-- 3.4.扣库存 incrby stockKey -1
redis.call('sadd', orderKey, userId)-- 3.5.下单（保存用户）sadd orderKey userId
redis.call('xadd', 'stream.orders', '*',
    'userId', userId, 'voucherId', voucherId, 'id', orderId)
-- 3.6.发送消息到队列中， XADD stream.orders * k1 v1 k2 v2 ...
return 0 --下单成功

--          >>> next Java stuff:
--接受消息由java开启线程任务,持续监听redis-stream