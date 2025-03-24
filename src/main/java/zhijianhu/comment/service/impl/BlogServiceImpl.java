package zhijianhu.comment.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import zhijianhu.comment.domain.Blog;
import zhijianhu.comment.domain.Follow;
import zhijianhu.comment.domain.User;
import zhijianhu.comment.dto.Result;
import zhijianhu.comment.dto.ScrollResult;
import zhijianhu.comment.dto.UserDTO;
import zhijianhu.comment.mapper.TbBlogMapper;
import zhijianhu.comment.service.IBlogService;
import zhijianhu.comment.service.IFollowService;
import zhijianhu.comment.service.IUserService;
import zhijianhu.comment.util.SystemConstants;
import zhijianhu.comment.util.UserHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
public class BlogServiceImpl extends ServiceImpl<TbBlogMapper, Blog> implements IBlogService {
    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;
    @Override
    public Result queryBlogById(Long id) {
        Blog blog = getById(id);
        if(blog==null){
            return Result.fail("帖子已被删除！");
        }
        getUserInfo(blog);
        return Result.ok(blog);
    }

    @Override
    public Result queryHot(Integer current) {
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(this::getUserInfo);
        return Result.ok(records);
    }

    @Override
    public void isLikePost(Long id) {
        UserDTO user = UserHolder.getUser();
        if(user==null) return;
        Long userId = user.getId();
        String key = BLOG_LIKED_KEY+id;
        Double success = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if(success==null){
            boolean update = update().setSql("liked=liked+1").eq("id", id).update();
            if(update){
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
        }else{
            boolean update = update().setSql("liked=liked-1").eq("id", id).update();
            if(update){
                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
        }
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key=BLOG_LIKED_KEY+id;
        Set<String> range = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        List<Long> ids = range.stream().map(Long::valueOf).toList();
        String join = StrUtil.join(",", ids);
        List<UserDTO> userList = userService.query()
                .in("id",ids).last("order by field(id,"+join+")")
                .list()
                .stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .toList();
        return Result.ok(userList);
    }

    @Override
    public Result saveBlog(Blog blog) {

        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean success = save(blog);
        if(!success){
            return Result.fail("发布帖子失败！");
        }
        //保存笔记之后，将消息推送给粉丝,获取粉丝列表
        List<Follow> followUserId = followService.query().eq("follow_user_id", user.getId()).list();
        for (Follow follow : followUserId){
            Long userId = follow.getUserId();
            String key=FEED_KEY+userId;
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(), System.currentTimeMillis());
        }


        // 返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        Long userId = UserHolder.getUser().getId();
        String key=FEED_KEY+userId;
//        查询收件箱
        Set<ZSetOperations.TypedTuple<String>> messages = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(
                        key,
                        0,
                        max,
                        offset,
                        2);
        if(messages==null||messages.isEmpty()){
            return Result.ok();
        }
        ArrayList<Long> ids = new ArrayList<>(messages.size());
        long minTim=0;
        int os=1;
        //解析
        for(ZSetOperations.TypedTuple<String> message : messages){
            ids.add(Long.valueOf(message.getValue()));
            long time = message.getScore().longValue();
            if(time==minTim){
                os++;
            }else{
                minTim=time;
                os=1;
            }
        }
        List<Blog> blogs = query()
                .in("id", ids)
                .last("order by field(id," + StrUtil.join(",", ids) + ")")
                .list();
        blogs.forEach(this::getUserInfo);
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setOffset(os);
        scrollResult.setMinTime(minTim);
        return Result.ok(scrollResult);
    }

    private void getUserInfo(Blog blog){
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        String key = BLOG_LIKED_KEY+blog.getId();
        UserDTO u = UserHolder.getUser();
        if(u!=null){
            Double score = stringRedisTemplate.opsForZSet()
                .score(key, u.getId().toString());
             blog.setIsLike(score!=null);
        }

        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
