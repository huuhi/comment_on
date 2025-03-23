package zhijianhu.comment.controller;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zhijianhu.comment.dto.Result;
import zhijianhu.comment.service.IVoucherOrderService;


/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {
    private final IVoucherOrderService voucherOrderService;
    @Lazy
    public VoucherOrderController(IVoucherOrderService voucherOrderService) {
        this.voucherOrderService = voucherOrderService;
    }

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
//        需要判断一下有没有库存，以及是否在开始的时间之内
        return voucherOrderService.seckillVoucher(voucherId);
    }
}
