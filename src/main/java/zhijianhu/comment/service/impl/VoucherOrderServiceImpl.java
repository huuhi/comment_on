package zhijianhu.comment.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<TbVoucherOrderMapper, VoucherOrder> implements
        IVoucherOrderService {
    private final ISeckillVoucherService seckillVoucherService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private static final DefaultRedisScript<Long> SEACH_SCRIPT;
    private final RedisIdWorker redisIdWorker;
//    private ApplicationContext applicationContext;
//    private IVoucherOrderService proxy;
    private final OrderTransactionService orderTransactionService;
    private final ShutdownCoordinator shutdownCoordinator;
    private static final ExecutorService ORDER_EXECUTOR = new ThreadPoolExecutor(
        4,
        10,
        60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1000),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );
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
        ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }


    private class VoucherOrderHandler implements Runnable{
        String queueName="stream.orders";
        @Override
        public void run() {
            while (!shutdownCoordinator.isShutdown()) {
                try {
                    List<MapRecord<String, Object, Object>> msg = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create(queueName, ReadOffset.lastConsumed())
                    );
                    if(msg==null||msg.isEmpty()){
                        continue;// 没有消息，继续下一次循环
                    }
                    MapRecord<String, Object, Object> record = msg.get(0);
                    Map<Object, Object> values = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);
                    stringRedisTemplate.opsForStream().acknowledge(queueName,"g1",record.getId());
                } catch (Exception e) {
                    log.error("处理订单有问题：{}",e.getMessage());
                    handlePendingList();
                }
            }

        }

        private void handlePendingList() {
            while (true) {
                try {
                    List<MapRecord<String, Object, Object>> msg = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );
                    if(msg==null||msg.isEmpty()){
                        break;// 说明pending-list 为空了
                    }
                    MapRecord<String, Object, Object> record = msg.get(0);
                    Map<Object, Object> values = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    handleVoucherOrder(voucherOrder);
                    stringRedisTemplate.opsForStream().acknowledge(queueName,"g1",record.getId());
                } catch (RedisSystemException e) {
                    log.error("消息处理异常：{}",e.getMessage());
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }

//    private final BlockingQueue<VoucherOrder> orderTasks=new ArrayBlockingQueue<>(1024*1024);
//    private class VoucherOrderHandler implements Runnable{
//        @Override
//        public void run() {
//            while (true) {
//                try {
//                    VoucherOrder take = orderTasks.take();
//                    handleVoucherOrder(take);
//                } catch (InterruptedException e) {
//                    log.error("处理订单有问题：{}",e.getMessage());
//                    throw new RuntimeException(e);
//                }
//            }
//
//        }
//    }

    private void handleVoucherOrder(VoucherOrder take) {
        Long userId = take.getUserId();
        RLock redisLock = redissonClient.getLock("lock:order:" + userId);
        boolean lock = redisLock.tryLock();
        if(!lock){
            log.debug("获取锁失败");
        }
//      获取当前线程的代理对象，如果直接调用this，事务不会生效
        try {
            orderTransactionService.createVoucherOrder(take);
        } finally {
            redisLock.unlock();// 释放锁
        }
    }


    public VoucherOrderServiceImpl(ISeckillVoucherService seckillVoucherService,
                                   StringRedisTemplate stringRedisTemplate,
                                   RedissonClient redissonClient,
                                   RedisIdWorker redisIdWorker, ShutdownCoordinator shutdownCoordinator, OrderTransactionService orderTransactionService) {
        this.seckillVoucherService = seckillVoucherService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
        this.redisIdWorker = redisIdWorker;
        this.shutdownCoordinator = shutdownCoordinator;
        this.orderTransactionService = orderTransactionService;
    }
    static{
        SEACH_SCRIPT=new DefaultRedisScript<>();
        SEACH_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SEACH_SCRIPT.setResultType(Long.class);
    }
    @Override
    public Result seckillVoucher(Long voucherId) {
        SeckillVoucher v = seckillVoucherService.getById(voucherId);
//        合理时间
        if(!reasonableTime(v)){
            return Result.fail("不在抢购时间");
        }
//        提前生成订单id，发送给lua脚本，如果有购买资格，直接添加到消息队列
        long orderId = redisIdWorker.nextId("order");
        Long userId = UserHolder.getUser().getId();
        //1.指向lua脚本
        Long execute = stringRedisTemplate.execute(SEACH_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(orderId)
        );
        assert execute != null;
        int r = execute.intValue();
        //2.判断结果是否为0
        if(r!=0){
        // 不为零，没有下单资格 1 没有库存 2 重复下单
             return Result.fail(r==1?"库存不足！":"重复下单！");
        }
//        获取代理对象
//        proxy= (IVoucherOrderService) AopContext.currentProxy();
        return Result.ok(orderId);
    }
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        SeckillVoucher v = seckillVoucherService.getById(voucherId);
////        合理时间
//        if(!reasonableTime(v)){
//            return Result.fail("不在抢购时间");
//        }
////        提前生成订单id，发送给lua脚本，如果有购买资格，直接添加到消息队列
//        Long userId = UserHolder.getUser().getId();
//        //1.指向lua脚本
//        Long execute = stringRedisTemplate.execute(SEACH_SCRIPT,
//                Collections.emptyList(),
//                voucherId.toString(),
//                userId.toString(),
//                String.valueOf(orderId)
//        );
//        assert execute != null;
//        int r = execute.intValue();
//        //2.判断结果是否为0
//        if(r!=0){
//        // 不为零，没有下单资格 1 没有库存 2 重复下单
//             return Result.fail(r==1?"库存不足！":"重复下单！");
//        }
//        //2.1为零下单，下单信息保存到堵塞队列
//        VoucherOrder voucherOrder = VoucherOrder.builder()
//            .id(orderId)
//            .voucherId(voucherId)
//            .userId(userId)
//            .build();
//        orderTasks.add(voucherOrder);
////        获取代理对象
//        proxy= (IVoucherOrderService)AopContext.currentProxy();
//        return Result.ok(orderId);
//    }

    private boolean reasonableTime(SeckillVoucher v) {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(v.getBeginTime()) && !now.isAfter(v.getEndTime());
    }

    @Transactional
    public void createVoucherOrder(VoucherOrder order){
        Long userId = order.getUserId();
        Long voucherId = order.getVoucherId();
        Long count = query().eq("user_id", userId)
                .eq("voucher_id", voucherId)
                .count();
        if(count>0){
            log.error("用户已经购买过一次了");
        }
        boolean update = seckillVoucherService.update()
                .setSql("stock=stock-1")
                .eq("voucher_id", voucherId)
//          乐观锁，判断数据是否被其他线程修改了数据，这里直接判断库存
                .gt("stock", 0)
                .update();
        if(!update){
            log.debug("库存不足");
        }
        save(order);
    }
}
