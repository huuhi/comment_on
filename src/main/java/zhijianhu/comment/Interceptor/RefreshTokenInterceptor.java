package zhijianhu.comment.Interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import zhijianhu.comment.dto.UserDTO;
import zhijianhu.comment.util.UserHolder;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static zhijianhu.comment.util.RedisConstants.LOGIN_USER_KEY;
import static zhijianhu.comment.util.RedisConstants.LOGIN_USER_TTL;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/3/17
 * 说明:拦截所有的请求
 */
@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {
    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    //    前置拦截器
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        获取token
        String token = request.getHeader("authorization");
        if(StrUtil.isBlank(token)){
            return true;
        }

        String key= LOGIN_USER_KEY + token;
//        从redis中获取用户信息
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(key);
//        校验是否存在
        if(map.isEmpty()){
            return true;
        }
        UserDTO user = BeanUtil.fillBeanWithMap(map, new UserDTO(), false);
        UserHolder.saveUser(user);
//        刷新token的有效期
        stringRedisTemplate.expire(key,LOGIN_USER_TTL, TimeUnit.MINUTES);

        return true;
    }

//    校验完成之后执行
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }


}
