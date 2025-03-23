package zhijianhu.comment.service;


import com.baomidou.mybatisplus.extension.service.IService;
import zhijianhu.comment.domain.VoucherOrder;
import zhijianhu.comment.dto.Result;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    Result seckillVoucher(Long voucherId);
}
