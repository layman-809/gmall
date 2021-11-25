package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Test
    void contextLoads() {
        //判断索引库是否存在
        if(!restTemplate.indexExists(Goods.class)){
            this.restTemplate.createIndex(Goods.class);
            this.restTemplate.putMapping(Goods.class);
        }

        Integer pageNum = 1;
        Integer pageSize = 100;
        //循环
        do {
            //分批查询spu
            ResponseVo<List<SpuEntity>> responseVo = this.pmsClient.querySpuByPageJson(new PageParamVo(pageNum, pageSize, null));
            //当前页的数据
            List<SpuEntity> spuEntities = responseVo.getData();
            //如果当页的spu集合为空,直接结束
            if(CollectionUtils.isEmpty(spuEntities)){
                return;
            }
            spuEntities.forEach(spuEntity -> {
                ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.querySkusBySpuId(spuEntity.getId());
                List<SkuEntity> skuEntities = skuResponseVo.getData();
                //如果sku的集合不为空，才需要把sku集合装化成goods集合
                if(!CollectionUtils.isEmpty(skuEntities)){
                    //只有sku不为空时，才需要查询品牌，如果sku为空了，就不需要转化为goods，就不需要品牌了
                   //因为同一个spu下,所有的sku的品牌都是一样的
                    ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(spuEntity.getBrandId());
                    BrandEntity brandEntity = brandEntityResponseVo.getData();
                    //查询分类
                    ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(spuEntity.getCategoryId());
                    CategoryEntity categoryEntity = categoryEntityResponseVo.getData();


                    //把每一个spu转化成goods对象
                    List<Goods> goodsList = skuEntities.stream().map(skuEntity -> {
                        Goods goods = new Goods();
                        goods.setSkuId(skuEntity.getId());
                        goods.setDefaultImage(skuEntity.getDefaultImage());
                        goods.setTitle(skuEntity.getTitle());
                        goods.setSubtitle(skuEntity.getSubtitle());
                        goods.setPrice(skuEntity.getPrice());

                        goods.setCreateTime(spuEntity.getCreateTime());
                        //查询库存信息
                        ResponseVo<List<WareSkuEntity>> wareSkuResponseVo = this.wmsClient.querySkuById(skuEntity.getId());
                        List<WareSkuEntity> wareSkuEntities = wareSkuResponseVo.getData();
                        if(!CollectionUtils.isEmpty(wareSkuEntities)){
                            goods.setSales(wareSkuEntities.stream().mapToLong(WareSkuEntity::getSales).reduce((a,b) -> a + b).getAsLong());
                            //任何仓库满足库存数量-已经锁定库存数量 > 0 就表示有货
                            goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));//库存
                        }
                        // 品牌
                        if(brandEntity != null){
                            goods.setBrandId(brandEntity.getId());
                            goods.setBrandName(brandEntity.getName());
                            goods.setLogo(brandEntity.getLogo());
                        }
                        //分类
                        if(categoryEntity != null){
                            goods.setCategoryId(categoryEntity.getId());
                            goods.setCategoryName(categoryEntity.getName());
                        }
                        List<SearchAttrValueVo> searchAttrValueVos = new ArrayList<>();
                        //销售类型的检索规格参数
                        ResponseVo<List<SkuAttrValueEntity>> saleSearchAttrResponseVo = this.pmsClient.querySearchAttrValueByCidAndSkuId(skuEntity.getCategoryId(), skuEntity.getId());
                        List<SkuAttrValueEntity> skuAttrValueEntities = saleSearchAttrResponseVo.getData();
                        //需要把List<SkuAttrValueEntity>转换成List<SearchAttrValueVo>最后放入searchAttrValueVos
                        if(!CollectionUtils.isEmpty(skuAttrValueEntities)){
                            searchAttrValueVos.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                BeanUtils.copyProperties(skuAttrValueEntity,searchAttrValueVo);
                                return searchAttrValueVo;
                            }).collect(Collectors.toList()));
                        }
                        //基本类型的检索规格参数
                        ResponseVo<List<SpuAttrValueEntity>> baseSearchResponseVo = this.pmsClient.querySearchAttrValueByCidAndSpuId(spuEntity.getCategoryId(), spuEntity.getId());
                        List<SpuAttrValueEntity> spuAttrValueEntities= baseSearchResponseVo.getData();
                        if(!CollectionUtils.isEmpty(spuAttrValueEntities)){
                            searchAttrValueVos.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                                SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                BeanUtils.copyProperties(spuAttrValueEntity,searchAttrValueVo);
                                return searchAttrValueVo;
                            }).collect(Collectors.toList()));
                        }
                        goods.setSearchAttrs(searchAttrValueVos);
                        return goods;
                    }).collect(Collectors.toList());
                    //批量保存
                    this.goodsRepository.saveAll(goodsList);
                }
            });
            //遍历当前页的spu的集合,查询spu下的skus
            pageNum++;
            pageSize = spuEntities.size();
        } while(pageSize == 100);
    }

}
