-- use for SimpleRedisLock
-- just for practice. product use Redisson
-- 比较线程标示与锁中的标示是否一致
--why lua? 确保下列代码批处理,保证了原子性
--keys,argv都是参数,
--  1.可以自定义参数
--  2.可以自定义参数量
if (redis.call('get', KEYS[1]) == ARGV[1]) then
    -- 释放锁 del key
    return redis.call('del', KEYS[1])
end
return 0