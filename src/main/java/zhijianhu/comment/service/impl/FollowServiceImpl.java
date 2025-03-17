package zhijianhu.comment.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import zhijianhu.comment.domain.Follow;
import zhijianhu.comment.mapper.TbFollowMapper;
import zhijianhu.comment.service.IFollowService;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<TbFollowMapper, Follow> implements IFollowService {

}
