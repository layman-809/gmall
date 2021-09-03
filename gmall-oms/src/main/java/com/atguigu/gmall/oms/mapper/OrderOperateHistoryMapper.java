package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderOperateHistoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单操作历史记录
 * 
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 23:39:54
 */
@Mapper
public interface OrderOperateHistoryMapper extends BaseMapper<OrderOperateHistoryEntity> {
	
}
