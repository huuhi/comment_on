package zhijianhu.comment.util;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/3/19
 * 说明:
 */
@Component
public class RedisIdWorker{
//     1970年1月1日 00:00:00 UTC到2025年3 月 19 日 00:00:00 UTC的秒数
    private static final long BEGIN_TIMESTAMP = 1742342400L;
    private static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix) {
//        生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond =  now.toEpochSecond( ZoneOffset.UTC);
        long timestamp = nowSecond-BEGIN_TIMESTAMP;

//        生成序列号 获取当前日期
        String format = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + format);
//          拼接返回
        return timestamp << COUNT_BITS | count;
    }
}
