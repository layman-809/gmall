package com.atguigu.gmall.pms.service.impl;

import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<CategoryEntity> queryCategory(Long pid) {
        //构造一个查询条件
        QueryWrapper<CategoryEntity> wrapper = new QueryWrapper<>();
        if(pid != -1){
            //0就是不显示
            wrapper.eq("parent_id",pid);
        }
        //1显示查询列表
        return this.list(wrapper);
    }

    @Override
    public List<CategoryEntity> queryCategoriesWithSubsByPid(Long pid) {

        return this.categoryMapper.queryCategoriesByPid(pid);
    }

    @Override
    public List<CategoryEntity> queryLvl123CatesByCid3(Long cid) {
        //根据三级分类的id查询三级分类
        CategoryEntity categoryEntity3 = this.getById(cid);
        //判断用户填写的id是否不存在
        if(categoryEntity3 == null){
            return null;
        }
        //根据三级分类查询父分类，二级分类
        CategoryEntity categoryEntity2 = this.getById(categoryEntity3.getParentId());

        //根据二级分类查询一级分类
        CategoryEntity categoryEntity1 = this.getById(categoryEntity2.getParentId());

        return Arrays.asList(categoryEntity1,categoryEntity2,categoryEntity3);
    }

}