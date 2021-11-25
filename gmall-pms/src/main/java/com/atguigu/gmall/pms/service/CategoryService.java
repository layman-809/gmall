package com.atguigu.gmall.pms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 21:05:49
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    List<CategoryEntity> queryCategory(Long pid);

    List<CategoryEntity> queryCategoriesWithSubsByPid(Long pid);

    List<CategoryEntity> queryLvl123CatesByCid3(Long cid);
}

