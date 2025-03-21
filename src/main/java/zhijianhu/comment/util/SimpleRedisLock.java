package zhijianhu.comment.util;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/3/21
 * 说明:
 */
public class SimpleRedisLock implements ILock{
    private final String name;
    private final StringRedisTemplate stringRedisTemplate;
    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name){
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.fastUUID().toString(true)+"-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }
    @Override
    public boolean tryLock(long timeoutSec) {
//        获取线程的名字
        String threadId =ID_PREFIX+ Thread.currentThread().getId();
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX+name, threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(b);
    }
        @Override
    public void unLock() {
            String thread=ID_PREFIX+Thread.currentThread().getId();
            stringRedisTemplate.execute(UNLOCK_SCRIPT,
                    Collections.singletonList(KEY_PREFIX+name),
                    thread);
    }

//    @Override
//    public void unLock() {
//        String thread =ID_PREFIX+Thread.currentThread().getId();
//        String s = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        if(thread.equals(s)){
//            stringRedisTemplate.delete(KEY_PREFIX+name);
//        }
//    }
}
