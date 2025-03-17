package zhijianhu.comment.service;


import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpSession;
import zhijianhu.comment.domain.User;
import zhijianhu.comment.dto.LoginFormDTO;
import zhijianhu.comment.dto.Result;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);
}
