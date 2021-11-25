package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 * 
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 21:05:49
 */
@Mapper
public interface SkuAttrValueMapper extends BaseMapper<SkuAttrValueEntity> {
    List<Map<String,Object>> queryAttrValuesMappingSkuId(@Param("skuIds") List<Long> skuIds);
}
