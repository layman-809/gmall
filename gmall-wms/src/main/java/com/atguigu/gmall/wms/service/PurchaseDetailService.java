package com.atguigu.gmall.wms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.wms.entity.PurchaseDetailEntity;

import java.util.Map;

/**
 * 
 *
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 23:57:05
 */
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

