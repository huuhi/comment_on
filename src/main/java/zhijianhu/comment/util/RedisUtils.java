package zhijianhu.comment.util;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static zhijianhu.comment.util.RedisConstants.CACHE_NULL_TTL;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/3/19
 * 说明:缓存工具类
 */
@Component
public class RedisUtils {
    private final StringRedisTemplate stringRedisTemplate;

    public RedisUtils(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     *
     * @param key 键
     * @param value 值
     * @param time 过期时间
     * @param unit 时间单位
     * @show 缓存数据
     */
    public void set(String key,Object value,Long time,TimeUnit unit){
        String json = JSONUtil.toJsonStr(value);
        stringRedisTemplate.opsForValue().set(key,json,time,unit);
    }

    /**
     * @param key        键
     * @param value      值
     * @param minutesTtl 过期时间，时间单位为分钟
     * @param unit 时间单位
     */
    public void setWithLogicalExpire(String key, Object value, Long minutesTtl, TimeUnit unit){
        RedisData data = new RedisData(LocalDateTime.now().plusSeconds(unit.toSeconds(minutesTtl)), value);
        String json = JSONUtil.toJsonStr(data);
        stringRedisTemplate.opsForValue().set(key,json);
    }

    /**
     *
     * @param keyPrefix 前缀键
     * @param id 查询的id
     * @param clazz 返回的类型
     * @param dbFallback 查询数据库的函数
     * @param time 过期时间
     * @param unit 时间单位
     * @return  返回指定的类型数据
     * @param <R> 返回的类型
     * @param <ID> 查询的id的类型
     * @author 胡志坚
     * @show 此方法解决了缓存穿透问题，在指定的时间过期上加1-10的随机值，防止缓存雪崩
     */
    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> clazz,
                                         Function<ID,R> dbFallback,Long time,
                                         TimeUnit unit){
 //      先查看是否有缓存,有则直接返回
        String key=keyPrefix+id ;
        String json = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(json)){
            return  JSONUtil.toBean(json, clazz);
        }
        if(json!=null){
            return null;
        }
//        没有则查询数据库
        R r=dbFallback.apply(id);
        if(r==null){
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
//       商铺详细信息过期时间为30分钟
//        随机一个随机的过期时间，防止缓存雪崩
        Integer random= RandomUtil.randomInt(1,10);
        this.set(key,r,time+random,unit);
        return r;
    }
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);


    //    逻辑过期解决缓存击穿问题

    /**
     *
     * @param keyPrefix 键前缀
     * @param id 查询的id
     * @param clazz 返回的类型
     * @param dbFallback 查询数据的函数
     * @param time 过期时间
     * @param unit 时间单位
     * @param lockKeyPrefix 锁的前缀
     * @return 返回指定的类型数据
     * @param <R> 返回类型
     * @param <ID> 查询的id的类型
     */
    public <R,ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> clazz,
                                         Function<ID,R> dbFallback,Long time,
                                         TimeUnit unit,String lockKeyPrefix){
        //      先查看是否有缓存,有则直接返回
        String key= keyPrefix+id;
        RedisData data = getRedisData(key);
        LocalDateTime expireTime = data.getExpireTime();
        JSONObject shopJson = (JSONObject) data.getData();
        R r = JSONUtil.toBean(shopJson, clazz);
        if(expireTime.isAfter(LocalDateTime.now())){
            return r;
        }
//        过期了获取锁，然后刷新缓存
        String lockKey = lockKeyPrefix + id;
        Boolean lock = lock(lockKey);
        if(!lock){
//            没有获取锁说明已经有线程去更新缓存了，这里直接返回旧数据
            return  r;
        }
//        创建一个线程刷新缓存,然后直接返回旧数据

//            doubleCheck 再次去查询一次缓存
        RedisData data2 = getRedisData(key);
        LocalDateTime expireTime2 =  data2.getExpireTime();
            JSONObject shopJson2 = (JSONObject) data2.getData();
            R r2 = JSONUtil.toBean(shopJson2, clazz);
            if(expireTime2.isAfter(LocalDateTime.now())){
                return r2;
            }
        CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    R r3 = dbFallback.apply(id);
                    this.setWithLogicalExpire(key,r3,time,unit);
                } finally {
                    unlock(lockKey);
                }
            });
        return r2;
    }

    private RedisData getRedisData(String key) {
        String s2 = stringRedisTemplate.opsForValue().get(key);
        return JSONUtil.toBean(s2, RedisData.class);
    }

    private Boolean lock(String key) {
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(b);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }


}
