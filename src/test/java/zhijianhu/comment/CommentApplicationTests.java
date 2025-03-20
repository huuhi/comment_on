package zhijianhu.comment;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import zhijianhu.comment.domain.Shop;
import zhijianhu.comment.service.impl.ShopServiceImpl;
import zhijianhu.comment.util.RedisIdWorker;
import zhijianhu.comment.util.RedisUtils;
import zhijianhu.comment.util.SystemConstants;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static zhijianhu.comment.util.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
@Slf4j
class CommentApplicationTests {
    @Autowired
    private ShopServiceImpl sp;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private StringRedisTemplate sbt;
    @Autowired
    private RedisIdWorker redisIdWorker;

    @Test
    void contextLoads() {
        Shop shop = sp.getById(1);
        redisUtils.setWithLogicalExpire(CACHE_SHOP_KEY + 1, shop, 10L, TimeUnit.SECONDS);
    }
    private final ExecutorService pool= Executors.newFixedThreadPool(300);

    @Test
    void getId() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task=()->{
            for (int i = 0; i < 100; i++) {
                long l = redisIdWorker.nextId("post");
                log.info("Id:{}",l);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            pool.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        log.debug("耗时：{} 毫秒", end - begin);
    }

}
