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
            //??????1????????????????????????????????? ??????2?????????????????????
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"},buildDsl(paramVo));
            //RequestOptions:??????????????????????????????????????????????????????
            SearchResponse response = this.highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            //???????????????????????????
            SearchResponseVo responseVo = parseResult(response);
            //?????????????????????????????????????????????
            responseVo.setPageNum(paramVo.getPageNum());
            responseVo.setPageSize(paramVo.getPageSize());

            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
            return null;
    }
    //?????????????????????
    private SearchResponseVo parseResult(SearchResponse response) {
        SearchResponseVo responseVo = new SearchResponseVo();
        //??????hits?????????
        SearchHits hits = response.getHits();
        //????????????
        responseVo.setTotal(hits.getTotalHits());
        //??????????????????
        SearchHit[] hitsHits = hits.getHits();

        responseVo.setGoodsList( Stream.of(hitsHits).map(searchHit ->{
            //??????_source??????????????????Goods????????????
            String json = searchHit.getSourceAsString();
            Goods goods = JSON.parseObject(json, Goods.class);
            //?????????????????????
            Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();
            //??????????????????
            HighlightField highlightField = highlightFields.get("title");
            goods.setTitle(highlightField.fragments()[0].string());

            return goods;
        }).collect(Collectors.toList()));

        //??????aggregation?????????
        Aggregations aggregations = response.getAggregations();
        ParsedLongTerms brandIdAgg = aggregations.get("brandIdAgg");
        //???????????????
        List<? extends Terms.Bucket> brandIdBuckets = brandIdAgg.getBuckets();
        //??????
        if(!CollectionUtils.isEmpty(brandIdBuckets)){
            //??????
            responseVo.setBrands( brandIdBuckets.stream().map(bucket -> { //?????????????????????????????????
                //???????????????
                BrandEntity brandEntity = new BrandEntity();
                //???????????????key????????????Id
                brandEntity.setId(bucket.getKeyAsNumber().longValue());
                //????????????
                //????????????????????????
                Aggregations subAggs = bucket.getAggregations();
                //??????????????????????????????
                ParsedStringTerms brandNameAgg = subAggs.get("brandNameAgg");
                //?????????????????????????????????
                List<? extends Terms.Bucket> nameBuckets = brandNameAgg.getBuckets();
                //??????????????????????????????????????????????????????
                if(!CollectionUtils.isEmpty(nameBuckets)){
                    //?????????????????????????????????????????????????????????
                    brandEntity.setName(nameBuckets.get(0).getKeyAsString());
                }
                //??????logo?????????
                ParsedStringTerms logoAgg = subAggs.get("logoAgg");
                //??????logo???????????????
                List<? extends Terms.Bucket> logoBuckets = logoAgg.getBuckets();
                //??????????????????????????????????????????????????????
                if(!CollectionUtils.isEmpty(logoBuckets)){
                    //logo?????????????????????????????????????????????
                    brandEntity.setLogo(logoBuckets.get(0).getKeyAsString());
                }
                return brandEntity;
            }).collect(Collectors.toList()));
        }
        //??????
        ParsedLongTerms categoryIdAgg = aggregations.get("categoryIdAgg");
        //?????????????????????
        List<? extends Terms.Bucket> categoryBuckets = categoryIdAgg.getBuckets();
        //??????
        if(!CollectionUtils.isEmpty(categoryBuckets)){
            //?????????????????????????????????
            responseVo.setCategories(categoryBuckets.stream().map(bucket -> {
                //???????????????
                CategoryEntity categoryEntity = new CategoryEntity();
                //???????????????key???????????????id
                categoryEntity.setId(bucket.getKeyAsNumber().longValue());
                //??????????????????????????????
                ParsedStringTerms categoryNameAgg = bucket.getAggregations().get("categoryNameAgg");
                //????????????????????????
                List<? extends Terms.Bucket> nameBuckets = categoryNameAgg.getBuckets();
                //??????????????????????????????????????????????????????
                if(!CollectionUtils.isEmpty(nameBuckets)){
                    //?????????????????????????????????????????????????????????
                    categoryEntity.setName(nameBuckets.get(0).getKeyAsString());
                }
                return categoryEntity;
            }).collect(Collectors.toList()));
        }
        //??????????????????????????????
        ParsedNested attrAgg = aggregations.get("attrAgg");
        //??????????????????????????????????????????id?????????
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        //????????????????????????
        List<? extends Terms.Bucket> attrBuckets = attrIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(attrBuckets)){
            //?????????????????????searchResponseAttrVo??????
            responseVo.setFilters(attrBuckets.stream().map(bucket -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                //???????????????key??????????????????id
                searchResponseAttrVo.setAttrId(bucket.getKeyAsNumber().longValue());
                //?????????????????????
                Aggregations subAggs = bucket.getAggregations();
                //????????????????????????????????????
                ParsedStringTerms attrNameAgg = subAggs.get("attrNameAgg");
                //??????????????????????????????????????????
                List<? extends Terms.Bucket> nameBuckets = attrNameAgg.getBuckets();
                //??????
                if(!CollectionUtils.isEmpty(nameBuckets)){
                    //??????????????????????????????????????????????????????????????????
                    searchResponseAttrVo.setAttrName(nameBuckets.get(0).getKeyAsString());
                }
                ParsedStringTerms attrValueAgg = subAggs.get("attrValueAgg");
                List<? extends Terms.Bucket> valueBuckets = attrValueAgg.getBuckets();
                if(!CollectionUtils.isEmpty(valueBuckets)){
                    //?????????????????????key
                    searchResponseAttrVo.setAttrValues(valueBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList()));
                }
                return searchResponseAttrVo;
            }).collect(Collectors.toList()));
        }
        return responseVo;

    }
    //??????????????????
    private SearchSourceBuilder buildDsl(SearchParamVo paramVo) {
        //?????????
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //??????keyword?????????????????????????????????
        String keyword = paramVo.getKeyword();
        //???????????????????????????????????????????????????????????????????????????????????????
        if(StringUtils.isBlank(keyword)){
            throw new RuntimeException("??????????????????????????????");
        }
        //1???????????????????????????
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);
        //1.1?????????????????????
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));
        //1.2???????????????????????????
        //1.2.1???????????????????????????
        List<Long> brandId = paramVo.getBrandId();
        if(!CollectionUtils.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brandId));
        }
        //1.2.2???????????????????????????
        List<Long> categoryId = paramVo.getCategoryId();
        if(!CollectionUtils.isEmpty(categoryId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId" , categoryId));
        }
        //1.2.3?????????????????????????????????
        List<String> props = paramVo.getProps();
        if(!CollectionUtils.isEmpty(props)){
            props.forEach(prop ->{
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                //???????????????
                String[] attr = StringUtils.split(prop, ":");
                //??????attr???????????????????????????2???????????????????????????
                if(attr != null && attr.length == 2){
                    // ScoreMode.None:???????????????null ?????????
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs",boolQuery, ScoreMode.None));
                    //?????????????????????????????????id
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId",attr[0]));
                    //??????????????????8G-12G,??????-??????
                    String[] attrValues = StringUtils.split(attr[1], "-");
                boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue",attrValues));
                }
            });
        }
        //1.2.4?????????????????????
        Boolean store = paramVo.getStore();
        if(store != null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("store",store));
        }
        //1.2.5???????????????????????????
        Double priceFrom = paramVo.getPriceFrom();
        Double priceTo = paramVo.getPriceTo();
        //??????????????????????????????????????????????????????????????????
        if(priceFrom != null || priceTo != null){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if(priceFrom != null){
                //????????????
//                boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(priceFrom));
                rangeQuery.gte(priceFrom);
            }
            if (priceTo != null){
                //????????????
//                boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").lte(priceTo));
                rangeQuery.lte(priceTo);
            }
            //???????????????????????????
//            boolQueryBuilder.filter(QueryBuilders.rangeQuery("price"));
            boolQueryBuilder.filter(rangeQuery);
        }
        //2?????????????????????
        //??????????????????
        Integer sort = paramVo.getSort();
        if(sort != null){
            //???????????????0/??????-???????????? 1-???????????? 2-???????????? 3-???????????? 4-????????????
            switch (sort){
                case 1: sourceBuilder.sort("price", SortOrder.DESC);break;
                case 2: sourceBuilder.sort("price",SortOrder.ASC);break;
                case 3: sourceBuilder.sort("sales",SortOrder.DESC);break;
                case 4: sourceBuilder.sort("createTime",SortOrder.DESC);break;
                default: sourceBuilder.sort("_score",SortOrder.DESC); break;
            }

        }
        //3?????????????????????
        Integer pageNum = paramVo.getPageNum();
        Integer pageSize = paramVo.getPageSize();
        sourceBuilder.from((pageNum-1)*pageSize);
        sourceBuilder.size(pageSize);
        //4???????????????
        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<em style='color:red;'>").postTags("</em>"));
        //5?????????????????????
        //5.1?????????????????????
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo")));
        //5.2????????????????????????
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));
        //5.3??????????????????????????????
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));

        //6??????????????????
//        sourceBuilder.fetchSource(new String[]{"skuId","title","subtitle","price","defaultImage"},null);
        System.out.println(sourceBuilder);
        return sourceBuilder;
    }
}
//http://localhost:18086/search?keyword=%E6%89%8B%E6%9C%BA&brandId=1&categoryId=225&priceFrom=1000&store=false&props=4:8G-12G&sort=1&pageNum=1