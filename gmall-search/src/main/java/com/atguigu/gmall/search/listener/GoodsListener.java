package com.atguigu.gmall.search.listener;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoodsListener {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GoodsRepository goodsRepository;

    //监听器
    @RabbitListener(bindings = @QueueBinding(//绑定交换机及队列
            //队列名称
            value = @Queue("SEARCH_INSERT_QUEUE"),
            //参数1：交换机名称
            //参数2：ignoreDeclarationExceptions: 忽略声明异常
            //参数3：交换机类型 RK支持通配符，通配模型
            exchange = @Exchange(value = "PMS_SPU_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            //交换机路由键
            key = ("item.insert")
    ))
    //supId:消息内容、channel:信道、messageL消息
    public void syncData(Long spuId, Channel channel, Message message) throws IOException {
        if (spuId == null) {//表示是一个垃圾消息
            //确认消息
            //getDeliveryTag：回传告诉rabbitmq此消息处理成功，清除此消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        //同步数据
        //根据spuId查询spu
        ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(spuId);
        SpuEntity spuEntity = spuEntityResponseVo.getData();
        if(spuEntity == null){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }
        //根据spuId查询spu下的所有sku
        ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.querySkusBySpuId(spuId);
        List<SkuEntity> skuEntities = skuResponseVo.getData();
        //如果sku的集合不为空，才需要把sku集合装化成goods集合
        if(!CollectionUtils.isEmpty(skuEntities)) {
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
                if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                    goods.setSales(wareSkuEntities.stream().mapToLong(WareSkuEntity::getSales).reduce((a, b) -> a + b).getAsLong());
                    //任何仓库满足库存数量-已经锁定库存数量 > 0 就表示有货
                    goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));//库存
                }
                // 品牌
                if (brandEntity != null) {
                    goods.setBrandId(brandEntity.getId());
                    goods.setBrandName(brandEntity.getName());
                    goods.setLogo(brandEntity.getLogo());
                }
                //分类
                if (categoryEntity != null) {
                    goods.setCategoryId(categoryEntity.getId());
                    goods.setCategoryName(categoryEntity.getName());
                }
                List<SearchAttrValueVo> searchAttrValueVos = new ArrayList<>();
                //销售类型的检索规格参数
                ResponseVo<List<SkuAttrValueEntity>> saleSearchAttrResponseVo = this.pmsClient.querySearchAttrValueByCidAndSkuId(skuEntity.getCategoryId(), skuEntity.getId());
                List<SkuAttrValueEntity> skuAttrValueEntities = saleSearchAttrResponseVo.getData();
                //需要把List<SkuAttrValueEntity>转换成List<SearchAttrValueVo>最后放入searchAttrValueVos
                if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                    searchAttrValueVos.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                        BeanUtils.copyProperties(skuAttrValueEntity, searchAttrValueVo);
                        return searchAttrValueVo;
                    }).collect(Collectors.toList()));
                }
                //基本类型的检索规格参数
                ResponseVo<List<SpuAttrValueEntity>> baseSearchResponseVo = this.pmsClient.querySearchAttrValueByCidAndSpuId(spuEntity.getCategoryId(), spuEntity.getId());
                List<SpuAttrValueEntity> spuAttrValueEntities = baseSearchResponseVo.getData();
                if (!CollectionUtils.isEmpty(spuAttrValueEntities)) {
                    searchAttrValueVos.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                        SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                        BeanUtils.copyProperties(spuAttrValueEntity, searchAttrValueVo);
                        return searchAttrValueVo;
                    }).collect(Collectors.toList()));
                }
                goods.setSearchAttrs(searchAttrValueVos);
                return goods;
            }).collect(Collectors.toList());
            //批量保存
            this.goodsRepository.saveAll(goodsList);
        }

        try {
            //手动确认消息
            //false表示不批量确认，如果设置成true在多个队列中会把该消息之前没确认或者出现问题的消息全部消费掉，一般使用false
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            //判断是否已经重试过
            if(message.getMessageProperties().getRedelivered()){
                //未重试，重新入队
                channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
            }else{
                //第三个参数表示是否重新入队，如果设置true，则重新入队
                //如果设置为false，则判断该队列有没有绑定死信队列，如果绑定了则进入死信队列，如果没绑定该消息将丢失。
                //不确认消息
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            }
        }
    }
}
