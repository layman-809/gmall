package com.atguigu.gmall.sms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;

import java.util.Map;

/**
 * 商品阶梯价格
 *
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 23:03:40
 */
public interface SkuLadderService extends IService<SkuLadderEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

