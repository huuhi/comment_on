package zhijianhu.comment.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import zhijianhu.comment.domain.Follow;

/**
* @author windows
* @description 针对表【tb_follow】的数据库操作Mapper
* @createDate 2025-03-16 19:06:12
* @Entity zhijianhu.comment.domain.TbFollow
*/
@Mapper
public interface TbFollowMapper extends BaseMapper<Follow> {

}




