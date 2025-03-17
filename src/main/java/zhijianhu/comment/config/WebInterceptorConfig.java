package zhijianhu.comment.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import zhijianhu.comment.Interceptor.LoginInterceptor;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/1/17
 * 说明:配置拦截器
 */
@Configuration//表示这是一个配置类
public class WebInterceptorConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //注册拦截器,排除登录页面
        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns(
                        "/user/login",
                        "/voucher/**",
                        "/user/code",
                        "/blog/hot",
                        "/shop-type/**",
                        "/upload/**",
                        "/shop/**"
                );
    }
}
