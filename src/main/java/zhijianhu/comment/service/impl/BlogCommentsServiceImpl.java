package zhijianhu.comment.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import zhijianhu.comment.domain.BlogComments;
import zhijianhu.comment.mapper.TbBlogCommentsMapper;
import zhijianhu.comment.service.IBlogCommentsService;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<TbBlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
