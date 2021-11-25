package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.util.QueryBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.querydsl.QuerydslUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient highLevelClient;


    public SearchResponseVo search(SearchParamVo paramVo) {
        try {
            //参数1：指定要搜索的索引库。 参数2：构建搜索条件
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"},buildDsl(paramVo));
            //RequestOptions:请求头信息，默认没有什么特殊的头信息
            SearchResponse response = this.highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            //解析出所需的结果集
            SearchResponseVo responseVo = parseResult(response);
            //分页结果集需要通过搜索参数获取
            responseVo.setPageNum(paramVo.getPageNum());
            responseVo.setPageSize(paramVo.getPageSize());

            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
            return null;
    }
    //解析搜索结果集
    private SearchResponseVo parseResult(SearchResponse response) {
        SearchResponseVo responseVo = new SearchResponseVo();
        //解析hits结果集
        SearchHits hits = response.getHits();
        //总记录数
        responseVo.setTotal(hits.getTotalHits());
        //当前页的记录
        SearchHit[] hitsHits = hits.getHits();

        responseVo.setGoodsList( Stream.of(hitsHits).map(searchHit ->{
            //获取_source，反序列化为Goods数据模型
            String json = searchHit.getSourceAsString();
            Goods goods = JSON.parseObject(json, Goods.class);
            //获取高亮结果集
            Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();
            //获取高亮字段
            HighlightField highlightField = highlightFields.get("title");
            goods.setTitle(highlightField.fragments()[0].string());

            return goods;
        }).collect(Collectors.toList()));

        //解析aggregation结果集
        Aggregations aggregations = response.getAggregations();
        ParsedLongTerms brandIdAgg = aggregations.get("brandIdAgg");
        //获取桶集合
        List<? extends Terms.Bucket> brandIdBuckets = brandIdAgg.getBuckets();
        //判空
        if(!CollectionUtils.isEmpty(brandIdBuckets)){
            //品牌
            responseVo.setBrands( brandIdBuckets.stream().map(bucket -> { //将桶集合转换为品牌集合
                //品牌实体类
                BrandEntity brandEntity = new BrandEntity();
                //每个桶中的key就是品牌Id
                brandEntity.setId(bucket.getKeyAsNumber().longValue());
                //品牌名称
                //获取所有的子聚合
                Aggregations subAggs = bucket.getAggregations();
                //获取品牌名称的子聚合
                ParsedStringTerms brandNameAgg = subAggs.get("brandNameAgg");
                //获取品牌名称子聚合的桶
                List<? extends Terms.Bucket> nameBuckets = brandNameAgg.getBuckets();
                //为了防止出现空指针异常这需要增加判空
                if(!CollectionUtils.isEmpty(nameBuckets)){
                    //品牌名称这个子聚合应该有且仅有一个元素
                    brandEntity.setName(nameBuckets.get(0).getKeyAsString());
                }
                //获取logo子聚合
                ParsedStringTerms logoAgg = subAggs.get("logoAgg");
                //获取logo子聚合的桶
                List<? extends Terms.Bucket> logoBuckets = logoAgg.getBuckets();
                //为了防止出现空指针异常这需要增加判空
                if(!CollectionUtils.isEmpty(logoBuckets)){
                    //logo这个子聚合应该有且仅有一个元素
                    brandEntity.setLogo(logoBuckets.get(0).getKeyAsString());
                }
                return brandEntity;
            }).collect(Collectors.toList()));
        }
        //分类
        ParsedLongTerms categoryIdAgg = aggregations.get("categoryIdAgg");
        //获取所有桶集合
        List<? extends Terms.Bucket> categoryBuckets = categoryIdAgg.getBuckets();
        //判空
        if(!CollectionUtils.isEmpty(categoryBuckets)){
            //把桶集合转换为分类集合
            responseVo.setCategories(categoryBuckets.stream().map(bucket -> {
                //分类实体类
                CategoryEntity categoryEntity = new CategoryEntity();
                //每个桶中的key就是分类的id
                categoryEntity.setId(bucket.getKeyAsNumber().longValue());
                //获取分类名称的子聚合
                ParsedStringTerms categoryNameAgg = bucket.getAggregations().get("categoryNameAgg");
                //获取分类名称的桶
                List<? extends Terms.Bucket> nameBuckets = categoryNameAgg.getBuckets();
                //为了防止出现空指针异常这需要增加判空
                if(!CollectionUtils.isEmpty(nameBuckets)){
                    //分类名称这个子聚合应该有且仅有一个元素
                    categoryEntity.setName(nameBuckets.get(0).getKeyAsString());
                }
                return categoryEntity;
            }).collect(Collectors.toList()));
        }
        //规格参数的聚合结果集
        ParsedNested attrAgg = aggregations.get("attrAgg");
        //获取嵌套聚合结果集中规格参数id子聚合
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        //获取子结果集的桶
        List<? extends Terms.Bucket> attrBuckets = attrIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(attrBuckets)){
            //将桶集合转化成searchResponseAttrVo集合
            responseVo.setFilters(attrBuckets.stream().map(bucket -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                //当前桶中的key就是规格参数id
                searchResponseAttrVo.setAttrId(bucket.getKeyAsNumber().longValue());
                //获取所有子聚合
                Aggregations subAggs = bucket.getAggregations();
                //获取规格参数名称的子聚合
                ParsedStringTerms attrNameAgg = subAggs.get("attrNameAgg");
                //获取规格参数名称子聚合中的桶
                List<? extends Terms.Bucket> nameBuckets = attrNameAgg.getBuckets();
                //判空
                if(!CollectionUtils.isEmpty(nameBuckets)){
                    //获取规格参数名称子聚合中的桶，有且仅只有一个
                    searchResponseAttrVo.setAttrName(nameBuckets.get(0).getKeyAsString());
                }
                ParsedStringTerms attrValueAgg = subAggs.get("attrValueAgg");
                List<? extends Terms.Bucket> valueBuckets = attrValueAgg.getBuckets();
                if(!CollectionUtils.isEmpty(valueBuckets)){
                    //获取子集合中的key
                    searchResponseAttrVo.setAttrValues(valueBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList()));
                }
                return searchResponseAttrVo;
            }).collect(Collectors.toList()));
        }
        return responseVo;

    }
    //构建搜索条件
    private SearchSourceBuilder buildDsl(SearchParamVo paramVo) {
        //结果集
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //通过keyword可以获取到要查询的字段
        String keyword = paramVo.getKeyword();
        //判断是否输入要查询的字段，未输入则不进行查询，返回提示信息
        if(StringUtils.isBlank(keyword)){
            throw new RuntimeException("搜索关键字不能为空！");
        }
        //1、构建查询过滤条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);
        //1.1、构建匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));
        //1.2、构建过滤条件查询
        //1.2.1、构建品牌过滤查询
        List<Long> brandId = paramVo.getBrandId();
        if(!CollectionUtils.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brandId));
        }
        //1.2.2、构建分类过滤查询
        List<Long> categoryId = paramVo.getCategoryId();
        if(!CollectionUtils.isEmpty(categoryId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId" , categoryId));
        }
        //1.2.3、构建规格参数过滤查询
        List<String> props = paramVo.getProps();
        if(!CollectionUtils.isEmpty(props)){
            props.forEach(prop ->{
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                //字符串分割
                String[] attr = StringUtils.split(prop, ":");
                //如果attr为空或者长度不等于2，表示输入的不合法
                if(attr != null && attr.length == 2){
                    // ScoreMode.None:的分模式为null 不查询
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs",boolQuery, ScoreMode.None));
                    //数组的第一个是规格参数id
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId",attr[0]));
                    //规格参数值：8G-12G,在以-分割
                    String[] attrValues = StringUtils.split(attr[1], "-");
                boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue",attrValues));
                }
            });
        }
        //1.2.4、构建是否有货
        Boolean store = paramVo.getStore();
        if(store != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("store",store));
        }
        //1.2.5、构建价格过滤查询
        Double priceFrom = paramVo.getPriceFrom();
        Double priceTo = paramVo.getPriceTo();
        //判断用户输入的价格是否为空或者是否只选择一项
        if(priceFrom != null || priceTo != null){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if(priceFrom != null){
                //大于等于
//                boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(priceFrom));
                rangeQuery.gte(priceFrom);
            }
            if (priceTo != null){
                //小于等于
//                boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").lte(priceTo));
                rangeQuery.lte(priceTo);
            }
            //大于等于和小于等于
//            boolQueryBuilder.filter(QueryBuilders.rangeQuery("price"));
            boolQueryBuilder.filter(rangeQuery);
        }
        //2、构建排序条件
        //获取排序字段
        Integer sort = paramVo.getSort();
        if(sort != null){
            //排序条件：0/其他-得分排序 1-价格降序 2-价格升序 3-销量降序 4-新品降序
            switch (sort){
                case 1: sourceBuilder.sort("price", SortOrder.DESC);break;
                case 2: sourceBuilder.sort("price",SortOrder.ASC);break;
                case 3: sourceBuilder.sort("sales",SortOrder.DESC);break;
                case 4: sourceBuilder.sort("createTime",SortOrder.DESC);break;
                default: sourceBuilder.sort("_score",SortOrder.DESC); break;
            }

        }
        //3、构建分页条件
        Integer pageNum = paramVo.getPageNum();
        Integer pageSize = paramVo.getPageSize();
        sourceBuilder.from((pageNum-1)*pageSize);
        sourceBuilder.size(pageSize);
        //4、构建高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<em style='color:red;'>").postTags("</em>"));
        //5、构建聚合条件
        //5.1、构建品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo")));
        //5.2、构建分类的聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));
        //5.3、构建规格参数的聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));

        //6、结果集过滤
//        sourceBuilder.fetchSource(new String[]{"skuId","title","subtitle","price","defaultImage"},null);
        System.out.println(sourceBuilder);
        return sourceBuilder;
    }
}
//http://localhost:18086/search?keyword=%E6%89%8B%E6%9C%BA&brandId=1&categoryId=225&priceFrom=1000&store=false&props=4:8G-12G&sort=1&pageNum=1