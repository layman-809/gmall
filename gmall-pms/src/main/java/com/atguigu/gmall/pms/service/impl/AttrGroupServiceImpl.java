package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.fasterxml.jackson.databind.util.BeanUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.AttrGroupMapper;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private AttrMapper attrMapper;
    @Autowired
    private SpuAttrValueMapper spuAttrValueMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrGroupEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<GroupVo> queryByCid(Long cid) {
        //根据分类id查询分组
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        //判断集合是否为空
        if(CollectionUtils.isEmpty(groupEntities)){
            return null;
        }
        //遍历所有分组查询分组下的规格参数
        //将attrGroupEntity集合转换成GroupEntity集合
       return groupEntities.stream().map(attrGroupEntity -> {
            //把每一个attrGroupEntity转换成groupVo
           GroupVo groupVo = new GroupVo();
            //把entity对象属性拷贝到groupVo
           BeanUtils.copyProperties(attrGroupEntity,groupVo);
           //查询每个分组下的规格参数
           List<AttrEntity> attrEntitys = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()).eq("type",1));
           //设置给分组的Vo对象
           groupVo.setAttrEntities(attrEntitys);
           return groupVo;
        }).collect(Collectors.toList());//转换成新的集合
    }

    @Override
    public List<ItemGroupVo> queryGroupWithAttrAndValuesByCidAndSkuIdAndSpuId(Long cid,  Long spuId, Long skuId) {
        //根据分类Id查询分组
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        if(CollectionUtils.isEmpty(groupEntities)){
            return null;
        }
        //遍历每个分组，查询每个分组下的规格参数
        return groupEntities.stream().map(attrGroupEntity -> {
            ItemGroupVo itemGroupVo = new ItemGroupVo();
            itemGroupVo.setId(attrGroupEntity.getId());//分组id
            itemGroupVo.setGroupName(attrGroupEntity.getName());//分组名称
            //根据分组id查询组下有哪些规格参数
            //attrEntities:规格参数实体类集合
            List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()));
            if(!CollectionUtils.isEmpty(attrEntities)){
                //获取attrIds集合
                List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());
                List<AttrValueVo> attrs = new ArrayList<>();
                //查询基本类型的规格参数和值
                List<SpuAttrValueEntity> spuAttrValueEntities = this.spuAttrValueMapper.selectList(new QueryWrapper<SpuAttrValueEntity>().eq("spu_id", spuId).in("attr_id", attrIds));
                if(!CollectionUtils.isEmpty(spuAttrValueEntities)){
                   attrs.addAll( spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                       AttrValueVo attrValueVo = new AttrValueVo();
                       //spuAttrValueEntity与attrValueVo参数名一样直接拷贝
                       BeanUtils.copyProperties(spuAttrValueEntity,attrValueVo);
                       return attrValueVo;
                   }).collect(Collectors.toList()));
                }
                //查询销售类型的规格参数和值
                List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIds));
                if(!CollectionUtils.isEmpty(skuAttrValueEntities)){
                    attrs.addAll( skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        //spuAttrValueEntity与attrValueVo参数名一样直接拷贝
                        BeanUtils.copyProperties(skuAttrValueEntity,attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList()));
                }
                itemGroupVo.setAttrs(attrs);//分组的规格参数
            }
            return itemGroupVo;
        }).collect(Collectors.toList());
    }

}