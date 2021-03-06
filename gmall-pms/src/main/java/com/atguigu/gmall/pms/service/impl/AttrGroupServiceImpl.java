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
        //????????????id????????????
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        //????????????????????????
        if(CollectionUtils.isEmpty(groupEntities)){
            return null;
        }
        //????????????????????????????????????????????????
        //???attrGroupEntity???????????????GroupEntity??????
       return groupEntities.stream().map(attrGroupEntity -> {
            //????????????attrGroupEntity?????????groupVo
           GroupVo groupVo = new GroupVo();
            //???entity?????????????????????groupVo
           BeanUtils.copyProperties(attrGroupEntity,groupVo);
           //????????????????????????????????????
           List<AttrEntity> attrEntitys = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()).eq("type",1));
           //??????????????????Vo??????
           groupVo.setAttrEntities(attrEntitys);
           return groupVo;
        }).collect(Collectors.toList());//?????????????????????
    }

    @Override
    public List<ItemGroupVo> queryGroupWithAttrAndValuesByCidAndSkuIdAndSpuId(Long cid,  Long spuId, Long skuId) {
        //????????????Id????????????
        List<AttrGroupEntity> groupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        if(CollectionUtils.isEmpty(groupEntities)){
            return null;
        }
        //?????????????????????????????????????????????????????????
        return groupEntities.stream().map(attrGroupEntity -> {
            ItemGroupVo itemGroupVo = new ItemGroupVo();
            itemGroupVo.setId(attrGroupEntity.getId());//??????id
            itemGroupVo.setGroupName(attrGroupEntity.getName());//????????????
            //????????????id?????????????????????????????????
            //attrEntities:???????????????????????????
            List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()));
            if(!CollectionUtils.isEmpty(attrEntities)){
                //??????attrIds??????
                List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());
                List<AttrValueVo> attrs = new ArrayList<>();
                //???????????????????????????????????????
                List<SpuAttrValueEntity> spuAttrValueEntities = this.spuAttrValueMapper.selectList(new QueryWrapper<SpuAttrValueEntity>().eq("spu_id", spuId).in("attr_id", attrIds));
                if(!CollectionUtils.isEmpty(spuAttrValueEntities)){
                   attrs.addAll( spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                       AttrValueVo attrValueVo = new AttrValueVo();
                       //spuAttrValueEntity???attrValueVo???????????????????????????
                       BeanUtils.copyProperties(spuAttrValueEntity,attrValueVo);
                       return attrValueVo;
                   }).collect(Collectors.toList()));
                }
                //???????????????????????????????????????
                List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueMapper.selectList(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIds));
                if(!CollectionUtils.isEmpty(skuAttrValueEntities)){
                    attrs.addAll( skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                        AttrValueVo attrValueVo = new AttrValueVo();
                        //spuAttrValueEntity???attrValueVo???????????????????????????
                        BeanUtils.copyProperties(skuAttrValueEntity,attrValueVo);
                        return attrValueVo;
                    }).collect(Collectors.toList()));
                }
                itemGroupVo.setAttrs(attrs);//?????????????????????
            }
            return itemGroupVo;
        }).collect(Collectors.toList());
    }

}