package zhijianhu.comment;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@SpringBootApplication(scanBasePackages = {"zhijianhu"})
@Slf4j
@MapperScan("zhijianhu.comment.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class CommentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentApplication.class, args);
        log.debug("项目启动");
    }

}
