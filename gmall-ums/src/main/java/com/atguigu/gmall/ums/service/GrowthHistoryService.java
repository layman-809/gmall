package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.ums.entity.GrowthHistoryEntity;

import java.util.Map;

/**
 * 成长积分记录表
 *
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 23:48:56
 */
public interface GrowthHistoryService extends IService<GrowthHistoryEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

