package zhijianhu.comment.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import zhijianhu.comment.domain.Blog;
import zhijianhu.comment.mapper.TbBlogMapper;
import zhijianhu.comment.service.IBlogService;

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

}
