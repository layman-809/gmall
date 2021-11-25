package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.mapper.SkuFullReductionMapper;
import com.atguigu.gmall.sms.mapper.SkuLadderMapper;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.sms.mapper.SkuBoundsMapper;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsMapper, SkuBoundsEntity> implements SkuBoundsService {

    @Autowired//满减优惠
    private SkuFullReductionMapper skuFullReductionMapper;
    @Autowired//打折优惠
    private SkuLadderMapper skuLadderMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuBoundsEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public void saleSkuSales(SkuSaleVo skuSaleVo) {
        /**
         *  3.1、保存积分优惠：sms_sku_bounds
         */
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        //拷贝SkuSaleVo属性到SkuBoundsEntity
        BeanUtils.copyProperties(skuSaleVo,skuBoundsEntity);
        //因为SkuSaleVo里的work是list类型而我们需要的是Integer类型，这里需要转换下
        List<Integer> work = skuSaleVo.getWork();
        if(CollectionUtils.isEmpty(work)){
            //将二进制装换成十进制
            skuBoundsEntity.setWork(work.get(3)*8 + work.get(2)*4 + work.get(1)*2 + work.get(0));
        }
        this.save(skuBoundsEntity);
        /**
         * 3.2、保存满减优惠：sms_sku_full_reduction
         */
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuSaleVo,skuFullReductionEntity);
        skuFullReductionEntity.setAddOther(skuSaleVo.getFullAddOther());
        this.skuFullReductionMapper.insert(skuFullReductionEntity);
        /**
         * 3.3、保存打折优惠：sms_sku_ladder
         */
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(skuSaleVo,skuLadderEntity);
        skuLadderEntity.setAddOther(skuSaleVo.getLadderAddOther());
        this.skuLadderMapper.insert(skuLadderEntity);
    }

    @Override
    public List<ItemSaleVo> querySalesBySkuId(Long skuId) {
        ArrayList<ItemSaleVo> itemSaleVos = new ArrayList<>();
        //积分优惠
        SkuBoundsEntity skuBoundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        if(skuBoundsEntity != null){
            ItemSaleVo itemSaleVo = new ItemSaleVo();
            //积分
            itemSaleVo.setType("积分");
            //描述信息
            itemSaleVo.setDesc("送" + skuBoundsEntity.getGrowBounds() + "成长积分，送" + skuBoundsEntity.getBuyBounds() + "购物积分");
            itemSaleVos.add(itemSaleVo);
        }
        //满减优惠
        SkuFullReductionEntity reductionEntity = this.skuFullReductionMapper.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        //如果不为空添加满减优惠
        if(reductionEntity != null){
            ItemSaleVo itemSaleVo = new ItemSaleVo();
            itemSaleVo.setType("满减");
            itemSaleVo.setDesc("满" + reductionEntity.getFullPrice()+"减"+reductionEntity.getReducePrice());
            itemSaleVos.add(itemSaleVo);
        }

        //打折优惠
        SkuLadderEntity ladderEntity = this.skuLadderMapper.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        if(ladderEntity != null){
            ItemSaleVo itemSaleVo = new ItemSaleVo();
            itemSaleVo.setType("打折");
            //divide(new BigDecimal(10))：因为打折优惠是百分比数据所以需要 / 10
            itemSaleVo.setDesc("满"+ ladderEntity.getFullCount() + "件，打" + ladderEntity.getDiscount().divide(new BigDecimal(10))+"折");
            itemSaleVos.add(itemSaleVo);
        }
        return null;
    }

}