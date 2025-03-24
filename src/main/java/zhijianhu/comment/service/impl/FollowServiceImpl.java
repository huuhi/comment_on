package zhijianhu.comment.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import jakarta.annotation.Resources;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import zhijianhu.comment.domain.Follow;
import zhijianhu.comment.dto.Result;
import zhijianhu.comment.dto.UserDTO;
import zhijianhu.comment.mapper.TbFollowMapper;
import zhijianhu.comment.service.IFollowService;
import zhijianhu.comment.service.IUserService;
import zhijianhu.comment.util.UserHolder;

import java.util.List;
import java.util.Set;

import static zhijianhu.comment.util.RedisConstants.FOLLOW_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<TbFollowMapper, Follow> implements IFollowService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        //1.如果isFollow是true 表示关注
        Long userId = UserHolder.getUser().getId();
        //1.1获取当前登录用户id，创造关注关系表，保存即可
        String key=FOLLOW_KEY+userId;
        if(isFollow){
            Follow build = Follow.builder()
                .followUserId(followUserId)
                .userId(userId)
                .build();
            boolean save = save(build);
            if(save){
                stringRedisTemplate.opsForSet().add(key,followUserId.toString());
            }
        }else{
            boolean remove = remove(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", followUserId));
            if(remove){
                stringRedisTemplate.opsForSet().remove(key,followUserId.toString());
            }
        }
        //2.如果是false，直接删除相关的关系数据即可
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        Long count = query().eq("user_id", userId)
                .eq("follow_user_id", followUserId)
                .count();
        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long id) {
        Long userId = UserHolder.getUser().getId();
        String key1=FOLLOW_KEY+userId;
        String key2=FOLLOW_KEY+id;
//        查询共同关注
        Set<String> union = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if(union==null||union.isEmpty()){
            return Result.ok(List.of());// 没有共同关注
        }
        List<Long> userIds = union.stream().map(Long::valueOf).toList();
        List<UserDTO> list = userService.listByIds(userIds)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .toList();
        return Result.ok(list);
    }
}
