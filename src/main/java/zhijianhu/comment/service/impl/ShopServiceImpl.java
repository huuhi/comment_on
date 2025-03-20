package zhijianhu.comment.service.impl;


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
import zhijianhu.comment.util.RedisData;
import zhijianhu.comment.util.RedisUtils;

import java.time.LocalDateTime;
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
    @Resource
    private RedisUtils redisUtils;


    @Override
    public Result getShop(Long id) {
//         解决缓存穿透问题
        Shop shop =redisUtils.queryWithPassThrough(
                CACHE_SHOP_KEY,id,Shop.class,
                this::getById,CACHE_SHOP_TTL, TimeUnit.MINUTES
        );

//        互斥锁解决缓存击穿问题
//        Shop shop = queryWithMutex(id);
//         逻辑删除解决缓存击穿问题
//        Shop shop=redisUtils.queryWithLogicalExpire(
//                CACHE_SHOP_KEY,id,Shop.class,
//                this::getById,CACHE_SHOP_TTL, TimeUnit.MINUTES,LOCK_SHOP_KEY
//        );

        if(shop==null){
            return Result.fail("商铺信息不存在！");
        }
        return Result.ok(shop);
    }
//    互斥锁解决缓存击穿问题
    private Shop queryWithMutex(Long id) {
         //      先查看是否有缓存,有则直接返回
        String key= CACHE_SHOP_KEY+id;
        Shop s=getShopByRedis(key);
        if(s!=null){
            return s;
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
            Shop shop1 = getShopByRedis(key);
            if(shop1!=null) return shop1;
        }else{
            try {
                Thread.sleep(100); // 等待一段时间
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Shop shopByRedis = getShopByRedis(key);
            if(shopByRedis!=null) return shopByRedis;
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
//    逻辑过期解决缓存击穿问题
    private Shop getShopByRedis(String key){
        String s = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(s)){
            return JSONUtil.toBean(s,Shop.class);
        }
        return null;
    }


    private String generateLockValue() {
        return UUID.randomUUID() + "-" + Thread.currentThread().getId();
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
        stringRedisTemplate.delete(key);
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
