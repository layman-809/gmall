package com.atguigu.gmall.index.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        //redis地址前需要加redis:// 或者 rediss://
        config.useSingleServer().setAddress("redis://192.168.61.130:6379");
        return Redisson.create(config);
    }
}
