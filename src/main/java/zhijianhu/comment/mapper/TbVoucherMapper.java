package zhijianhu.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import zhijianhu.comment.domain.Voucher;

import java.util.List;

/**
* @author windows
* @description 针对表【tb_voucher】的数据库操作Mapper
* @createDate 2025-03-16 19:06:38
* @Entity zhijianhu.comment.domain.TbVoucher
*/
public interface TbVoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfShop(@Param("shopId") Long shopId);
}




