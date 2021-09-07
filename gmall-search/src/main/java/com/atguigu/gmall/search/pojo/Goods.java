package com.atguigu.gmall.search.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 一个goods对应一个SKU
 */
@Data
@Document(indexName = "goods" , shards = 3 , replicas = 2)
public class Goods {
    //商品列表所需字段
    @Id
    private Long skuId;
    //默认分词不需要索引
    @Field(type = FieldType.Keyword,index = false)
    private String defaultImage;
    @Field(type = FieldType.Double)
    private BigDecimal price;
    @Field(type = FieldType.Text,analyzer = "ik_max_word")
    private String title;
    @Field(type =FieldType.Keyword ,index = false )
    private String subtitle;

    //排序所需字段
    @Field(type = FieldType.Integer)
    private Long sales = 0l;//销量
    @Field(type = FieldType.Date)
    private Date createTime;//新品

    //过滤字段
    @Field(type = FieldType.Boolean)
    private Boolean store = false;//是否有货 默认无货

    //聚合相关的字段
    @Field(type = FieldType.Long)
    private Long brandId;//品牌Id
    @Field(type = FieldType.Keyword)
    private String brandName;//品牌名称
    @Field(type = FieldType.Keyword)
    private String logo;//品牌logo

    //分类字段
    @Field(type = FieldType.Long)
    private Long categoryId;//分类id
    @Field(type = FieldType.Keyword)
    private String categoryName;//分类名称
    @Field(type = FieldType.Nested)//嵌套类型
    private List<SearchAttrValueVo> searchAttrs;//检索类型的规格参数

}
