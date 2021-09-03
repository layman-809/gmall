package com.atguigu.gmall.wms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;

import java.util.Map;

/**
 * 商品库存
 *
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 23:57:05
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

