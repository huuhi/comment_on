package zhijianhu.comment.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import zhijianhu.comment.domain.Blog;

/**
* @author windows
* @description 针对表【tb_blog】的数据库操作Mapper
* @createDate 2025-03-16 19:05:30
* @Entity zhijianhu.comment.domain.TbBlog
*/
@Mapper
public interface TbBlogMapper extends BaseMapper<Blog> {

}




