package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 商品三级分类
 * 
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 21:05:49
 */
@Mapper
public interface CategoryMapper extends BaseMapper<CategoryEntity> {
    //当参数只有一个时@Param可以不写
    List<CategoryEntity> queryCategoriesByPid(Long pid);

}
