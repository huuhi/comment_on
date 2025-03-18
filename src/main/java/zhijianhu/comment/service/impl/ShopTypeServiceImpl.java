package zhijianhu.comment.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import zhijianhu.comment.domain.ShopType;
import zhijianhu.comment.dto.Result;
import zhijianhu.comment.mapper.TbShopTypeMapper;
import zhijianhu.comment.service.IShopTypeService;

import java.util.List;

import static zhijianhu.comment.util.RedisConstants.SHOP_TYPE_KEY;

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
public class ShopTypeServiceImpl extends ServiceImpl<TbShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Result getShopTypeList() {
//        查询商店列表,先去redis查询
        List<String> list = stringRedisTemplate.opsForList().range(SHOP_TYPE_KEY, 0, -1);
        if(list!=null&&!list.isEmpty()){
            List<ShopType> shopTypeList = list.stream()
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json, ShopType.class);
                        } catch (JsonProcessingException e) {
                            log.error("反序列化失败:{}",e.getMessage());
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
            return Result.ok(shopTypeList);
        }
//        如果没有缓存则去数据库中查询
        List<ShopType> sort = query().orderByAsc("sort").list();
        if(!sort.isEmpty()){
            List<String> collect = sort.stream()
                    .map(JSONUtil::toJsonStr)
                    .toList();
//            将集合转换为字符串数组，然后存入redis中
            stringRedisTemplate.opsForList().rightPushAll(SHOP_TYPE_KEY, collect.toArray(String[]::new));
        }
        return Result.ok(sort);
    }
}
