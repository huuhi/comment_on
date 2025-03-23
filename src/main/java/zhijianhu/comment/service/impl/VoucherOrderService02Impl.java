package zhijianhu.comment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import zhijianhu.comment.domain.SeckillVoucher;
import zhijianhu.comment.domain.VoucherOrder;
import zhijianhu.comment.dto.Result;
import zhijianhu.comment.mapper.TbVoucherOrderMapper;
import zhijianhu.comment.service.ISeckillVoucherService;
import zhijianhu.comment.service.IVoucherOrderService;
import zhijianhu.comment.util.RedisIdWorker;
import zhijianhu.comment.util.UserHolder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/3/23
 * 说明:
 */
@Service
@Slf4j
@Primary
public class VoucherOrderService02Impl extends ServiceImpl<TbVoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    private final ISeckillVoucherService seckillVoucherService;
    private final OrderTransactionService orderTransactionService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private static final DefaultRedisScript<Long> SEACH_SCRIPT;
    private final RedisIdWorker redisIdWorker;
    private final ShutdownCoordinator shutdownCoordinator;
    private static final ExecutorService ORDER_EXECUTOR = new ThreadPoolExecutor(
            10,
            20,
            60,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(3),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );
    public VoucherOrderService02Impl(ISeckillVoucherService seckillVoucherService, OrderTransactionService orderTransactionService, StringRedisTemplate stringRedisTemplate, RedissonClient redissonClient, RedisIdWorker redisIdWorker, ShutdownCoordinator shutdownCoordinator) {
        this.seckillVoucherService = seckillVoucherService;
        this.orderTransactionService = orderTransactionService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
        this.redisIdWorker = redisIdWorker;
        this.shutdownCoordinator = shutdownCoordinator;
    }
    static{
        SEACH_SCRIPT = new DefaultRedisScript<>();
        SEACH_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SEACH_SCRIPT.setResultType(Long.class);
    }
//    出现异常
     @PreDestroy
    public void shutdown() {
            // 分阶段关闭
        ORDER_EXECUTOR.shutdown(); // 温和关闭
        try {
            if (!ORDER_EXECUTOR.awaitTermination(15, TimeUnit.SECONDS)) {
                ORDER_EXECUTOR.shutdownNow(); // 强制关闭
            }
        } catch (InterruptedException e) {
            ORDER_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("订单线程池已关闭");
    }
    //    这个注解的作用是：在spring容器启动时，执行这个方法
    @PostConstruct
    public void init(){
        ORDER_EXECUTOR.submit(new VoucherHandleOrder());
    }

//    内部类，处理消息队列！
    private class VoucherHandleOrder implements Runnable{
        String queueName = "stream.orders";
        @Override
        public void run() {
            while(true){
                try {
                    List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    if(records==null||records.isEmpty()){
                        continue;
                    }
                    MapRecord<String, Object, Object> record = records.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);
                    stringRedisTemplate.opsForStream().acknowledge(queueName,"g1",record.getId());
                } catch (Exception e) {
                    readPendingList();
                    throw new RuntimeException(e);
                }
            }

        }
    private void readPendingList() {
         while(true){
            try {
                List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                        Consumer.from("g1", "c1"),
                        StreamReadOptions.empty().count(1),
                        StreamOffset.create(queueName, ReadOffset.from("0"))
                );
                if(records==null||records.isEmpty()){
                    break;
                }
                MapRecord<String, Object, Object> record = records.get(0);
                Map<Object, Object> value = record.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                handleVoucherOrder(voucherOrder);
                stringRedisTemplate.opsForStream().acknowledge(queueName,"g1",record.getId());
            } catch (Exception e) {
                log.error("处理pendingList错误：{}",e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
}
    private void handleVoucherOrder(VoucherOrder v){
        Long userId = v.getUserId();
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean success = lock.tryLock();
        if(!success){
            log.debug("获取锁失败");
        }
        try {
            orderTransactionService.createVoucherOrder(v);
        }finally {
            lock.unlock();
        }
    }
    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(seckillVoucher.getBeginTime()) || now.isAfter(seckillVoucher.getEndTime())) {
            return Result.fail("不在秒杀时间段内");
        }
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");
        Long result = stringRedisTemplate.execute(
                SEACH_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(orderId)
        );
        assert result != null;
        int r=result.intValue();
        if(r!=0){
            return Result.fail(r==1?"库存不足":"重复下单！");
        }
        return Result.ok(orderId);
    }
}
