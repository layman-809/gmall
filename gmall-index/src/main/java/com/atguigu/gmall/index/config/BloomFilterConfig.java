package com.atguigu.gmall.index.config;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.fegin.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Configuration
public class BloomFilterConfig {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private GmallPmsClient gmallPmsClient;
    //缓存前缀
    private static final String KEY_PREFIX = "index:cates:";

    @Bean
    public RBloomFilter<String> bloomFilter(){
        //初始化布隆过滤器
        //参数：指定前缀
        RBloomFilter<String> bloomFilter = this.redissonClient.getBloomFilter("index:BloomFilter");
        //自定义元素个数和精确度
        bloomFilter.tryInit(500,0.03);
        //查询所有一级分类
        ResponseVo<List<CategoryEntity>> listResponseVo = this.gmallPmsClient.queryCategory(0l);
        //获取一级分类列表
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        //判空
        if(!CollectionUtils.isEmpty(categoryEntities)){
            //遍历一级分类
            categoryEntities.forEach(categoryEntity -> {
                //向布隆过滤器添加数据
                bloomFilter.add(KEY_PREFIX+ categoryEntity.getId());
            });
        }
        return bloomFilter;
    }
}
