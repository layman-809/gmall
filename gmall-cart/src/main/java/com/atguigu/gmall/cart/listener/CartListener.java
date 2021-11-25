package com.atguigu.gmall.cart.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
public class CartListener {
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private CartMapper cartMapper;
    //价格前缀
    private static final String PRICE_PREFIX = "cart:price:";
    private static final String KEY_PREFIX = "cart:info:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("CART_PRICE_QUEUE"),
            exchange = @Exchange(value = "PMS_SPU_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"item.update"}
    ))
    public void syncPrice(Long spuId , Channel channel , Message message) throws IOException {
        if(spuId == null){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }
        //根据skuId查询spu下的所有的sku
        ResponseVo<List<SkuEntity>> listResponseVo = this.pmsClient.querySkusBySpuId(spuId);
        List<SkuEntity> skuEntities = listResponseVo.getData();
        if(CollectionUtils.isEmpty(skuEntities)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }
        //不为空遍历sku集合同步价格
        skuEntities.forEach(skuEntity -> {
            //setIfPresent：有才会设置，没有不会设置
            //购物车中有该条数据才会覆盖
            this.redisTemplate.opsForValue().setIfPresent(PRICE_PREFIX + skuEntity.getId(), skuEntity.getPrice().toString());
        });
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);//确认消息
    }


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("CART_DELETE_QUEUE"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"cart.delete"}
    ))
    public void deleteCart(Map<String , Object>map,Channel channel,Message message) throws IOException {
        if(CollectionUtils.isEmpty(map)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
        }
            Long userId = (Long) map.get("userId");
            List<String> skuIds = JSON.parseArray(map.get("skuIds").toString(), String.class);

            if (userId == null || CollectionUtils.isEmpty(skuIds)){
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
                return;
            }
            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
            hashOps.delete(skuIds.toArray());

            this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id",userId).in("sku_id",skuIds));
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
