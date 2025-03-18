package zhijianhu.comment.service;


import com.baomidou.mybatisplus.extension.service.IService;
import zhijianhu.comment.domain.Shop;
import zhijianhu.comment.dto.Result;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    Result getShop(Long id);

    Result updateShopAndDelCache(Shop shop);
}
