package zhijianhu.comment.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zhijianhu.comment.domain.SeckillVoucher;
import zhijianhu.comment.domain.VoucherOrder;
import zhijianhu.comment.dto.Result;
import zhijianhu.comment.mapper.TbVoucherOrderMapper;
import zhijianhu.comment.service.ISeckillVoucherService;
import zhijianhu.comment.service.IVoucherOrderService;
import zhijianhu.comment.util.RedisIdWorker;
import zhijianhu.comment.util.SimpleRedisLock;
import zhijianhu.comment.util.UserHolder;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static zhijianhu.comment.util.RedisConstants.LOCK_ORDER_TTL;
import static zhijianhu.comment.util.RedisConstants.SECKILL_STOCK_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<TbVoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    private final ISeckillVoucherService seckillVoucherService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;;
    public VoucherOrderServiceImpl(ISeckillVoucherService seckillVoucherService, StringRedisTemplate stringRedisTemplate, RedissonClient redissonClient) {
        this.seckillVoucherService = seckillVoucherService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
    }

    @Override
    public Result seckillVoucher(Long voucherId) {
//      1.  获取优惠券的id
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        LocalDateTime beginTime = voucher.getBeginTime();
        LocalDateTime endTime = voucher.getEndTime();
//        2.判断是否在秒杀时间段内
        if(beginTime.isAfter(LocalDateTime.now())||endTime.isBefore(LocalDateTime.now())){
            return Result.fail("秒杀尚未开始或已经结束");
        }
//        3.判断是否有库存
        Integer stock = voucher.getStock();
        if(stock<1){
            return Result.fail("已经抢购完了~");
        }
        Long userId = UserHolder.getUser().getId();
//        SimpleRedisLock redisLock = new SimpleRedisLock(stringRedisTemplate, "order:" + userId);
//        boolean lock = redisLock.tryLock(LOCK_ORDER_TTL);
        RLock redisLock = redissonClient.getLock("lock:order:" + userId);
        boolean lock = redisLock.tryLock();
        if(!lock){
            return Result.fail("重复下单！");
        }
//            获取当前线程的代理对象，如果直接调用this，事务不会生效
        try {
            IVoucherOrderService IVOS = (IVoucherOrderService)AopContext.currentProxy();
            return IVOS.createVoucherOrder(voucherId,userId);
        } finally {
            redisLock.unlock();// 释放锁
        }

    }
    @Transactional
    public Result createVoucherOrder(Long voucherId,Long userId){
        Long count = query()
                .eq("user_id", userId)
                .eq("voucher_id", voucherId)
                .count();
        if (count > 0) {
            return Result.fail("你已经购买过一次了！");
        }
        boolean update = seckillVoucherService.update()
                .setSql("stock=stock-1")
                .eq("voucher_id", voucherId)
//          乐观锁，判断数据是否被其他线程修改了数据，这里直接判断库存
                .gt("stock", 0)
                .update();
        if (!update) {
            return Result.fail("库存不足");
        }
//      4.1 用全局id生成器获取id
        RedisIdWorker redisIdWorker = new RedisIdWorker(stringRedisTemplate);
        long orderId = redisIdWorker.nextId(SECKILL_STOCK_KEY);
        VoucherOrder voucherOrder = VoucherOrder.builder()
                .id(orderId)
                .voucherId(voucherId)
                .createTime(LocalDateTime.now())
                .userId(userId)
                .build();
        save(voucherOrder);
        return Result.ok(orderId);
    }
}
