package com.atguigu.gmall.search.repository;

import com.atguigu.gmall.search.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

//Long 主键类型
public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
}
