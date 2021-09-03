package com.atguigu.gmall.oms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.oms.entity.OrderSettingEntity;

import java.util.Map;

/**
 * 订单配置信息
 *
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 23:39:54
 */
public interface OrderSettingService extends IService<OrderSettingEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

