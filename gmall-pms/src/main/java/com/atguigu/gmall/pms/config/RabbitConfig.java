package com.atguigu.gmall.pms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import javax.annotation.PostConstruct;

@Configuration
@Slf4j
public class RabbitConfig {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct//在构造方法之后执行
    public void init() {
        //确认回调，是否到达交换机
        this.rabbitTemplate.setConfirmCallback((correlationData,  ack, cause) -> {
            if (!ack) {
                log.error("消息没有到达交换机。。。" + cause);//cause:原因
            }
        });
        //确认回调，消息是否到达队列，到达队列该方法不执行
        this.rabbitTemplate.setReturnCallback(( message, replyCode, replyText, exchange,  routingKey) -> {
            log.error("消息没有到达队列：交换机{},路由键{},消息内容{}",exchange,routingKey,new String(message.getBody()));
         });
     }
}
