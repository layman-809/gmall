package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.entity.SpuEntity;
import lombok.Data;

import java.util.List;

@Data//继承SpuEntity的属性字段进行扩展
public class SpuVo extends SpuEntity {
    //图片信息
    private List<String> spuImages;
    //基本属性信息
    private List<SpuAttrValueVo> baseAttrs;
    //sku信息
    private List<SkuVO> skus;
}
