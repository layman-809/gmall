package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {

    // 面包屑三级分类
    private List<CategoryEntity> categories;

    // 面包屑品牌
    private Long brandId;
    private String brandName;

    // 面包屑spu
    private Long spuId;
    private String spuName;

    //商品详情页 主体部分
    private Long skuId;
    private String title;//标题
    private String subTitle;//副标题
    private BigDecimal price;//价格
    private Integer weight;//重量
    private String defaultImage;//默认图片

    // sku图片列表
    private List<SkuImagesEntity> images;

    // 营销信息sms
    private List<ItemSaleVo> sales;

    // 是否有货
    private Boolean store = false;

    // sku所属spu下的所有sku的销售属性
    // [{attrId: 3, attrName: '颜色', attrValues: '白色','黑色','粉色'},
    // {attrId: 8, attrName: '内存', attrValues: '6G','8G','12G'},
    // {attrId: 9, attrName: '存储', attrValues: '128G','256G','512G'}]
    private List<SaleAttrValueVo> saleAttrs;

    // 当前sku的销售属性：{3:'白色',8:'8G',9:'128G'}
    private Map<Long, String> saleAttr;

    //销售属性组合和skuId的映射关系
    // sku列表：{'白色,8G,128G': 4, '白色,8G,256G': 5, '白色,8G,512G': 6, '白色,12G,128G': 7}
    private String skuJsons;

    // spu的海报信息
    private List<String> spuImages;

    // 规格参数组及组下的规格参数(带值)
    private List<ItemGroupVo> groups;
}