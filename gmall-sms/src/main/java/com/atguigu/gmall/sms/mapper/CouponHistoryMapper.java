package com.atguigu.gmall.sms.mapper;

import com.atguigu.gmall.sms.entity.CouponHistoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券领取历史记录
 * 
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 23:03:41
 */
@Mapper
public interface CouponHistoryMapper extends BaseMapper<CouponHistoryEntity> {
	
}
