package com.atguigu.gmall.sms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.sms.entity.CouponHistoryEntity;

import java.util.Map;

/**
 * 优惠券领取历史记录
 *
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 23:03:41
 */
public interface CouponHistoryService extends IService<CouponHistoryEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

