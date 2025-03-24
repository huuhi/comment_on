package zhijianhu.comment.service;


import com.baomidou.mybatisplus.extension.service.IService;
import zhijianhu.comment.domain.Blog;
import zhijianhu.comment.dto.Result;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    Result queryBlogById(Long id);

    Result queryHot(Integer current);

    void isLikePost(Long id);

    Result queryBlogLikes(Long id);

    Result saveBlog(Blog blog);

    Result queryBlogOfFollow(Long max, Integer offset);
}
