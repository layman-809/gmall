package com.atguigu.gmall.search.pojo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParamVo {
    //搜索关键字
    private String keyword;
    //品牌过滤条件
    private List<Long> brandId;
    //分类的过滤条件
    private List<Long> categoryId;
    //规格参数过滤条件["4:8G-12G","5:128G-256G-512G"]
    private List<String> props;
    //排序条件：0-得分排序1-价格降序2-价格升序3-销量降序4-新品降序
    private Integer sort;
    //价格范围过滤条件
    private Double priceFrom;
    private Double priceTo;

    //分页参数
    private Integer pageNum = 1;
    private final Integer pageSize = 20;

    //是否有货
    private Boolean store;


}
