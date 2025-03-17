package zhijianhu.comment.Interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import zhijianhu.comment.domain.User;
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
//        获取session
        HttpSession session = request.getSession();
//        获取session中的用户
        Object user = session.getAttribute("user");
//        校验是否存在
        if (user == null) {
//            拦截并且返回 401 状态码 表示未授权
            response.setStatus(401);
            return false;
        }
        UserHolder.saveUser((UserDTO) user);
        return true;
    }

//    校验完成之后执行
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
