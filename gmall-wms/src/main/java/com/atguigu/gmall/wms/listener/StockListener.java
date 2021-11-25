package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;

@Component
public class StockListener {

    @Autowired
    private WareSkuMapper wareSkuMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "stock:info:";

    @RabbitListener(bindings =@QueueBinding(
            value = @Queue("STOCK_UNLOCK_QUEUE"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"stock.unlock"}
    ))
    public void unlock(String orderToken, Channel channel, Message message) throws IOException {
      if(StringUtils.isBlank(orderToken)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
            return;
      }

        String json = this.redisTemplate.opsForValue().get(KEY_PREFIX + orderToken);
      if(StringUtils.isBlank(json)){
          channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
          return;
      }
        List<SkuLockVo> skuLockVos = JSON.parseArray(json, SkuLockVo.class);
      if(CollectionUtils.isEmpty(skuLockVos)){
          channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
          return;
      }
      skuLockVos.forEach(skuLockVo -> {
          this.wareSkuMapper.unlockStock(skuLockVo.getSkuId(),skuLockVo.getCount());
      });

      this.redisTemplate.delete(KEY_PREFIX+orderToken);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
