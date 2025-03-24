package zhijianhu.comment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import zhijianhu.comment.domain.Follow;
import zhijianhu.comment.dto.Result;


/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    Result follow(Long followUserId, Boolean isFollow);

    Result isFollow(Long followUserId);

    Result followCommons(Long id);
}
