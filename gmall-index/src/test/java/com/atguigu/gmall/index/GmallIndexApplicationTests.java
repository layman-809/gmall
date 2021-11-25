package com.atguigu.gmall.index;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class GmallIndexApplicationTests {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Test
    void contextLoads() {
        //获取bloom过滤器
        RBloomFilter<Object> bloomFilter = this.redissonClient.getBloomFilter("bloomFilter");
        //参数1：元素个数
        //参数2：误判率
        bloomFilter.tryInit(20,0.3);
        //添加元素
        bloomFilter.add("1");
        bloomFilter.add("2");
        bloomFilter.add("3");
        bloomFilter.add("4");
        bloomFilter.add("5");
        bloomFilter.add("6");
        //contains:判断元素是否存在
        System.out.println(bloomFilter.contains("2"));
        System.out.println(bloomFilter.contains("4"));
        System.out.println(bloomFilter.contains("6"));
        System.out.println(bloomFilter.contains("8"));
        System.out.println(bloomFilter.contains("10"));
        System.out.println(bloomFilter.contains("11"));
        System.out.println(bloomFilter.contains("12"));
        System.out.println(bloomFilter.contains("13"));
        System.out.println(bloomFilter.contains("14"));
        System.out.println(bloomFilter.contains("15"));
        System.out.println(bloomFilter.contains("16"));
    }

}
