package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 21:05:49
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {
    PageResultVo queryPage(PageParamVo paramVo);

    List<GroupVo> queryByCid( Long cid);

    List<ItemGroupVo> queryGroupWithAttrAndValuesByCidAndSkuIdAndSpuId(Long cid, Long spuId, Long skuId);
}

