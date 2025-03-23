-- 1. 优惠价id
local voucherId=ARGV[1];
-- 2.用户的id
local userId=ARGV[2];
-- 3.订单id
local orderId=ARGV[3];

-- key
-- 库存的key
local stockKey='seckill:stock:' .. voucherId;

-- 订单的key
local orderKey='seckill:order:' .. userId;

-- 判断库存是否充足
if(tonumber(redis.call('get',stockKey)) <=0 ) then
    -- 库存不足，直接删除
    redis.call('setex', stockKey, 300, 0)
    return 1;
end
-- 判断用户是否下单
if(redis.call('sismember',orderKey,userId) == 1) then
    return 2;
end
-- 扣减库存
redis.call('incrby',stockKey,-1)
-- 下单
redis.call('sadd',orderKey,userId)
-- 添加到消息队列中
redis.call('xadd','stream.orders','*','voucherId',voucherId,'userId',userId,'id',orderId)

return 0;
