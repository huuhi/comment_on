package zhijianhu.comment.service.impl;


import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zhijianhu.comment.domain.Shop;
import zhijianhu.comment.dto.Result;
import zhijianhu.comment.mapper.TbShopMapper;
import zhijianhu.comment.service.IShopService;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static zhijianhu.comment.util.RedisConstants.*;

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
public class ShopServiceImpl extends ServiceImpl<TbShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result getShop(Long id) {
        Shop shop = queryWithMutex(id);
        if(shop==null){
            return Result.fail("商铺信息不存在！");
        }
        return Result.ok(shop);
    }
    private Shop queryWithMutex(Long id) {
         //      先查看是否有缓存,有则直接返回
        String key= CACHE_SHOP_KEY+id;
        String s = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(s)){
            return  JSONUtil.toBean(s, Shop.class);
        }

        if(s!=null){
            return null;
        }
//      实现缓存重构
//       1.获取互斥锁
        String lockKey= LOCK_SHOP_KEY+id;
        int retryCount = 0;
        final int MAX_RETRY_COUNT = 5; // 最大重试次数
        boolean lock = false;
        while(retryCount<MAX_RETRY_COUNT){
            lock = lock(lockKey);
            if(lock){
                break;
            }
            retryCount++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
//        Double Check 再检查一下缓存存不存在
        if(lock){
            String s2 = stringRedisTemplate.opsForValue().get(key);
            if(StrUtil.isNotBlank(s2)){
                return  JSONUtil.toBean(s2, Shop.class);
            }
        }else{
            try {
                Thread.sleep(100); // 等待一段时间
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            String s3 = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(s3)) {
                return JSONUtil.toBean(s3, Shop.class);
            }
            throw new RuntimeException("获取锁失败，请稍后重试");
        }
        Shop shop=null;
        // 2.判断是否获取锁成功 ->如果成功需要判断有没有缓存，失败则休眠再次尝试
        try {
//            成功，尝试获取缓存
            shop = getById(id);
            if(shop==null){
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
//       商铺详细信息过期时间为30分钟
//        随机一个随机的过期时间，防止缓存雪崩
            Integer random= RandomUtil.randomInt(1,10);
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL+random, TimeUnit.MINUTES);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        } finally {
//            解锁
            unlock(lockKey);
        }
        return  shop;
    }


    private Shop queryWithPassThrough(Long id){
        //      先查看是否有缓存,有则直接返回
        String key= CACHE_SHOP_KEY+id;
        String s = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(s)){
            return  JSONUtil.toBean(s, Shop.class);
        }
        if(s!=null){
            return null;
        }
//        没有则查询数据库
        Shop shop = getById(id);
        if(shop==null){
            stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
//       商铺详细信息过期时间为30分钟
//        随机一个随机的过期时间，防止缓存雪崩
        Integer random= RandomUtil.randomInt(1,10);
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL+random, TimeUnit.MINUTES);
        return  shop;
    }


    private String generateLockValue() {
        return UUID.randomUUID().toString() + "-" + Thread.currentThread().getId();
    }

    private Boolean lock(String key) {
        String lockValue = generateLockValue();
        return Boolean.TRUE.equals(
            stringRedisTemplate.opsForValue().setIfAbsent(
                key, lockValue, LOCK_SHOP_TTL, TimeUnit.SECONDS
            )
        );
    }

    private void unlock(String key) {
        String lockValue = stringRedisTemplate.opsForValue().get(key);
        String currentLockValue = generateLockValue();
        // 使用 Lua 脚本保证原子性
        String script =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "   return redis.call('del', KEYS[1]) " +
            "else " +
            "   return 0 " +
            "end";
        stringRedisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList(key),
            currentLockValue
        );
    }

    @Override
    @Transactional// 添加事务，保证原子性
    public Result updateShopAndDelCache(Shop shop) {
        Long id = shop.getId();
        if(id==null){
            return Result.fail("店铺id不能为空");
        }
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }
}
