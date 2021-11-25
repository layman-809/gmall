package com.atguigu.gmall.item.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.ItemException;
import com.atguigu.gmall.item.config.ThreadPoolConfig;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private ExecutorService executorService;
    @Autowired
    private TemplateEngine templateEngine;

    public ItemVo loadData(Long skuId) {
        ItemVo itemVo = new ItemVo();

//        1.根据skuId查询sku : 商品详情页
        CompletableFuture<SkuEntity> skuFuture = CompletableFuture.supplyAsync(() -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                throw new ItemException("对应的商品不存在");
            }
            itemVo.setSkuId(skuId);
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setDefaultImage(skuEntity.getDefaultImage());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setWeight(skuEntity.getWeight());
            return skuEntity;
        }, executorService);
//        2.根据三级分类的id查询一二三级分类
        CompletableFuture<Void> catesFuture = skuFuture.thenAcceptAsync(skuEntity -> {//不需要给下一个任务返回结果集
            ResponseVo<List<CategoryEntity>> categoryResponseVo = this.pmsClient.queryLvl123CatesByCid3(skuEntity.getCategoryId());
            List<CategoryEntity> categoryEntities = categoryResponseVo.getData();
            itemVo.setCategories(categoryEntities);
        }, executorService);//executorService:指定的线程池
//        3.根据品牌id查询品牌
        CompletableFuture<Void> brandsFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        }, executorService);

//        4.根据spuId查询spu
        CompletableFuture<Void> spuFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity != null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        }, executorService);
//        5.根据skuId查询sku的图片列表
        CompletableFuture<Void> imagesFuture = CompletableFuture.runAsync(() -> { //不依赖任何任务也不需要返回结果集 （）无参
            ResponseVo<List<SkuImagesEntity>> imagesResponseVo = this.pmsClient.queryImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntities = imagesResponseVo.getData();
            itemVo.setImages(skuImagesEntities);
        }, executorService);
//        6.根据skuId查询sku的营销信息
        CompletableFuture<Void> salesFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(skuId);
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            itemVo.setSales(itemSaleVos);
        }, executorService);
//        7.根据skuId查询库存列表
        CompletableFuture<Void> wareFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<WareSkuEntity>> wareSkuResponseVo = this.wmsClient.querySkuById(skuId);
            List<WareSkuEntity> wareSkuEntities = wareSkuResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                //wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0
                //当前库存减去被锁定的库存大于0表示有货
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
        }, executorService);
//        8.根据spuId查询spu下所有sku的销售属性
        CompletableFuture<Void> saleAttrsFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<SaleAttrValueVo>> saleAttrsResponseVo = this.pmsClient.querySaleAttrValueBySpuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> saleAttrValueVos = saleAttrsResponseVo.getData();
            itemVo.setSaleAttrs(saleAttrValueVos);
        }, executorService);
//        9.根据skuId查询当前sku的销售属性
        CompletableFuture<Void> saleAttrFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.pmsClient.querySkuAttrValuesBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                itemVo.setSaleAttr(skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue)));
            }
        }, executorService);
//        10.根据spuId查询spu下所有销售属性组合 和 skuId 映射关系
        CompletableFuture<Void> mappingFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<String> stringResponseVo = this.pmsClient.queryMappingBySpuId(skuEntity.getSpuId());
            String json = stringResponseVo.getData();
            itemVo.setSkuJsons(json);
        }, executorService);
//        11.根据spuId查询spu的描述信息
        CompletableFuture<Void> descFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            if (spuDescEntity != null) {
                itemVo.setSpuImages(Arrays.asList(StringUtils.split(spuDescEntity.getDecript(), ",")));
            }
        }, executorService);
//        12.根据cid、spuId、skuId查询分组及组下的规格参数和值
        CompletableFuture<Void> groupFuture = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<ItemGroupVo>> groupsResponseVo = this.pmsClient.queryGroupWithAttrAndValuesByCidAndSkuIdAndSpuId(skuEntity.getCategoryId(), skuEntity.getSpuId(), skuId);
            List<ItemGroupVo> groupVos = groupsResponseVo.getData();
            itemVo.setGroups(groupVos);
        }, executorService);
        CompletableFuture.allOf(catesFuture,brandsFuture,spuFuture,
                                imagesFuture,salesFuture,wareFuture,
                                saleAttrsFuture,saleAttrFuture,mappingFuture,
                                descFuture,groupFuture).join();
        //如果没有静态页面 请求进入网关 进入微服务获取加载动态页面并生成静态页面缓存到nginx，下次请求直接访问静态页面
        this.asyncExecute(itemVo);
        return itemVo;
    }
    //异步执行方法
    public void asyncExecute(ItemVo itemVo){
        executorService.execute(() -> {
            this.generateHtml(itemVo);
        });
    }

    public void generateHtml(ItemVo itemVo){
        //文件流生成到哪个目录下，每个文件以skyId区分,jdk1.8新特性 不需要在判空，会自动释放流
        try( PrintWriter printWriter = new PrintWriter("D:\\recv\\0325班\\html\\" + itemVo.getSkuId() + ".html")) {
            //上下文对象，可以通过它给模板传递动态数据
            Context context = new Context();
            //给模板传递的数据 参数1：数据模型的名字 参数2：数据模型的值
            context.setVariable("itemVo", itemVo);

            //页面静态化方法：参数 1-模板名称 2-上下文对象 3-文件流
            this.templateEngine.process("item",context,printWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }
}
