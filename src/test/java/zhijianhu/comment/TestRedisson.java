package zhijianhu.comment;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/3/22
 * 说明:
 */
//@SpringBootTest
@Slf4j
public class TestRedisson {
    @Resource
    private RedissonClient redissonClient;
    private RLock lock;

    @BeforeEach
    void setUp() {
        lock=redissonClient.getLock("order");
    }
    @Test
    void test1(){
        boolean b = lock.tryLock();
        if(!b){
            log.debug("获取锁失败1");
            return;
        }
        try{
            log.debug("获取锁成功1" );
            test2();
        }finally {
            lock.unlock();
        }
    }
    void test2(){
        boolean b = lock.tryLock();
        if(!b){
            log.debug("获取锁失败2");
            return;
        }
        try{
            log.debug("获取锁成功2" );
        }finally {
            lock.unlock();
        }
    }

}
