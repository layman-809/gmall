package com.atguigu.gmall.gateway.filter;

import lombok.Data;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class XxxGatewayFilterFactory extends AbstractGatewayFilterFactory<XxxGatewayFilterFactory.KeyValueConfig>{

    public XxxGatewayFilterFactory(){//用来接收参数
        super (KeyValueConfig.class);
    }
    //指定接收参数字段顺序
    @Override
    public List<String> shortcutFieldOrder() {
        //字段顺序
        return Arrays.asList("params");
    }
    //指定的接收类型(集合)
    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    @Override
    public GatewayFilter apply(KeyValueConfig config) {

        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                System.out.println("我是局部过滤器，我拦截特定路由的请求: params="+config.params );
                //放心
                return chain.filter(exchange);
            }
        };
    }
    @Data
    public static class KeyValueConfig{
        //接收固定数量参数
//        private String key;
//        private String value;
        //接收不固定数量参数
        private List<String> params;
    }
}
