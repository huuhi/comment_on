package zhijianhu.comment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zhijianhu.comment.domain.VoucherOrder;
import zhijianhu.comment.mapper.TbVoucherOrderMapper;
import zhijianhu.comment.service.ISeckillVoucherService;

@Service
@Slf4j
public class OrderTransactionService {
    
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    
    @Autowired
    private TbVoucherOrderMapper voucherOrderMapper;

    @Transactional
    public void createVoucherOrder(VoucherOrder order) {
        Long userId = order.getUserId();
        Long voucherId = order.getVoucherId();
        
        // 检查重复下单
        Long count = voucherOrderMapper.selectCount(new QueryWrapper<VoucherOrder>()
                .eq("user_id", userId)
                .eq("voucher_id", voucherId));
        if (count > 0) {
            log.debug("用户已经购买过一次了");
        }
        
        // 扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            log.debug("库存不足");
        }
        // 保存订单
        voucherOrderMapper.insert(order);
    }
}