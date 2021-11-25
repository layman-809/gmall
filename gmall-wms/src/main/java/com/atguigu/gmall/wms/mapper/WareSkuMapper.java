package com.atguigu.gmall.wms.mapper;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 23:57:05
 */
@Mapper
public interface WareSkuMapper extends BaseMapper<WareSkuEntity> {
    // 验库存方法
    List<WareSkuEntity> checkStock(@Param("skuId") Long skuId, @Param("count") Integer count);

    // 锁库存
    int lockStock(@Param("id") Long id, @Param("count") Integer count);

    // 解库存
    int unlockStock(@Param("id") Long id, @Param("count") Integer count);

}
