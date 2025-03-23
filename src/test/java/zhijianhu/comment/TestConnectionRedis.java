package zhijianhu.comment;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/3/23
 * 说明:
 */
@SpringBootTest
@Slf4j
public class TestConnectionRedis {
    @Autowired
    private StringRedisTemplate s;

    @Test
    void test() {
        s.opsForValue().set("name","HU");
        System.out.println(s.opsForValue().get("name"));
    }

}
