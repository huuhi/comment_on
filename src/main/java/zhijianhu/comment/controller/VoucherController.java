package zhijianhu.comment.controller;


import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import zhijianhu.comment.domain.Voucher;
import zhijianhu.comment.dto.Result;
import zhijianhu.comment.service.IVoucherService;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private IVoucherService voucherService;

    /**
     * 新增普通券
     * @param voucher 优惠券信息
     * @return 优惠券id
     */
    @PostMapping
    public Result addVoucher(@RequestBody Voucher voucher) {
        voucherService.save(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 新增秒杀券
     * @param voucher 优惠券信息，包含秒杀信息
     * @return 优惠券id
     */
    @PostMapping("/seckill")
//    抢购是写死的  限时抢购是我们添加的券
//    需要把时间设置成我们的范围   点击限时抢购就会弹出功能未完成
//    401一般是你路径写错 拦截器拦住 或者可能是请求头auth没写（不过我没写也能成功）
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 查询店铺的优惠券列表
     * @param shopId 店铺id
     * @return 优惠券列表
     */
    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
       return voucherService.queryVoucherOfShop(shopId);
    }
}
