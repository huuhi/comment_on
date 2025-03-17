package zhijianhu.comment.Interceptor;



import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import zhijianhu.comment.dto.UserDTO;
import zhijianhu.comment.util.UserHolder;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/3/17
 * 说明:拦截器
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {


    //    前置拦截器
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserDTO user = UserHolder.getUser();
        if(user==null){
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
