package zhijianhu.comment.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import zhijianhu.comment.domain.BlogComments;

/**
* @author windows
* @description 针对表【tb_blog_comments】的数据库操作Mapper
* @createDate 2025-03-16 19:06:00
* @Entity zhijianhu.comment.domain.TbBlogComments
*/
@Mapper
public interface TbBlogCommentsMapper extends BaseMapper<BlogComments> {

}




