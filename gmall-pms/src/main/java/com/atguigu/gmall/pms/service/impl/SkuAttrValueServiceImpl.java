package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuMapper;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
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

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import springfox.documentation.spring.web.json.Json;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {
    @Autowired
    private AttrMapper attrMapper;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<SkuAttrValueEntity> querySearchAttrValueByCidAndSkuId(Long cid, Long skuId) {
        //1、根据cid查询销售类型检索类型的规格参数
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category_id", cid)//分组Id
                    .eq("search_type", 1)//是否需要检索的
                    .eq("type", 0);//销售类型
        List<AttrEntity> attrEntities = this.attrMapper.selectList(queryWrapper);
        //判空
        if (CollectionUtils.isEmpty(attrEntities)) {
            return null;
        }
        //获取规格参数attrIds集合
        List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());
        //2、根据skuId结合上一步查询到的attrIds查值
        return this.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id",skuId).in("attr_id",attrIds));

    }

    @Override
    public List<SaleAttrValueVo> querySaleAttrValueBySpuId(Long spuId) {
        //1、根据spuId查询spu下所有的sku
        List<SkuEntity> skuEntities = this.skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id",spuId));
        if(CollectionUtils.isEmpty(skuEntities)){
            return null;
        }
        //获取sku集合,将skuEntities变为新的只包含id的集合
        List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());
        //2、根据skuIds查询销售属性
        //QueryWrapper<>().in():此处最好是List入参，in才更为精准
        List<SkuAttrValueEntity> skuAttrValueEntities = this.list(new QueryWrapper<SkuAttrValueEntity>().in("sku_id", skuIds));
        if(CollectionUtils.isEmpty(skuAttrValueEntities)){
            return null;
        }

        //创建一个新的集合
        ArrayList<SaleAttrValueVo> saleAttrValueVos = new ArrayList<>();
        //3、转化为List<SaleAttrValueVo>并以attrId进行分组
        //map的key就是attrId，value就是List<SkuAttrValueEntity>
        Map<Long, List<SkuAttrValueEntity>>  map = skuAttrValueEntities.stream().collect(Collectors.groupingBy(SkuAttrValueEntity::getAttrId));
        //把每一个K V 结构转化成{attrId: 3, attrName: '颜色', attrValues: '白色','黑色','粉色'}
        map.forEach((attrId,atrValueEntities) ->{
            SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
            saleAttrValueVo.setAttrId(attrId);
            saleAttrValueVo.setAttrName(atrValueEntities.get(0).getAttrName());
            saleAttrValueVo.setAttrValues(atrValueEntities.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet()));

            saleAttrValueVos.add(saleAttrValueVo);
        });
        return saleAttrValueVos;
    }

    @Override
    public String queryMappingBySpuId(Long spuId) {
        //根据spuId查询spu下所有的sku
        List<SkuEntity> skuEntities = this.skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id",spuId));
        if(CollectionUtils.isEmpty(skuEntities)){
            return null;
        }
        //获取skuId集合
        List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());
        //查询映射关系
        List<Map<String, Object>> maps = this.skuAttrValueMapper.queryAttrValuesMappingSkuId(skuIds);
        if(CollectionUtils.isEmpty(maps)){
            return null;
        }
        //把{sku_id = 19 ,attr_value = 白色，8G,512G}中attr_values作为新的map中的key
        Map<String, Long> mappingMap = maps.stream().collect(Collectors.toMap(map -> map.get("attr_values").toString(), map -> (Long)map.get("sku_id")));
        //序列化JSON字符串
        return JSON.toJSONString(mappingMap);
    }



}