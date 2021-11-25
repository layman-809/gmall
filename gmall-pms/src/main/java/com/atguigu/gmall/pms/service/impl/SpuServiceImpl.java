package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.mapper.*;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.SkuVO;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescMapper spuDescMapper;
    @Autowired
    private SpuAttrValueService spuAttrValueService;
    @Autowired
    private SkuMapper skuMapper;
    @Autowired
    private SkuImagesService skuImagesService;
    @Autowired
    private SkuAttrValueService skuAttrValueService;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private SpuDescService descService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo querySpuByCidAndPage(Long categoryId, PageParamVo pageParamVo) {
        //构建查询条件
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        //判断分类id不等于零，需要查询本类
        if(categoryId != 0){
            wrapper.eq("category_id" ,categoryId);
        }
        //关键字
        String key = pageParamVo.getKey();
        //判断输入框的内容是否为空
        if(StringUtils.isNotBlank(key)){
            //默认条件之间是and关系，如果需要or，可以在条件之间添加一个or()方法
            //SQL select * from pms_spu where category_id and (id=7 or name like '%7%')
            wrapper.and(t -> t.eq("id" ,key).or().like("name",key));
        }
        IPage<SpuEntity> page = this.page(
                pageParamVo.getPage(),
                wrapper
        );

        return new PageResultVo(page);
    }


    @Override
    public void bigSave(SpuVo spu) {

        /** 1、保存spu相关信息
         *  1.1、保存spu pms_spu
         */
        Long spuId = saveSpu(spu);
        /**
         * 1.2、保存pms_spu_deso(描述信息)
         */
        this.descService.saveSpuDesc(spu, spuId);

        /**
         * 1.3、保存基本属性值pms_attr_value
         */
        saveBaseAttrs(spu, spuId);

        /**2、保存sku相关信息
         * 2.1、保存sku pms_sku
         */
        saveSku(spu, spuId);
        //convertAndSend：转换和发送消息
        //参数1：交换机的名称
        //参数2：路由键
        //参数3：发送的内容
        this.rabbitTemplate.convertAndSend("PMS_SPU_EXCHANGE","item.insert",spuId);
    }

    private void saveSku(SpuVo spu, Long spuId) {
        List<SkuVO> skus = spu.getSkus();
        if(CollectionUtils.isEmpty(skus)){
            return;
        }
        //遍历每一个SkuVo保存对应的信息
        skus.forEach(skuVO -> {
            skuVO.setSpuId(spuId);
            skuVO.setCategoryId(spu.getCategoryId());
            skuVO.setBrandId(spu.getBrandId());
            List<String> images = skuVO.getImages();
            //判断图片信息是否为空
            if(!CollectionUtils.isEmpty(images)){
                //有默认图片取默认图片，没有默认图片取第一张为默认图片
                skuVO.setDefaultImage(StringUtils.isBlank(skuVO.getDefaultImage()) ? images.get(0) : skuVO.getDefaultImage());
            }
            this.skuMapper.insert(skuVO);
            Long skuId = skuVO.getId();
            /**
             * 2.2、保存sku图片pms_sku_images
             */
            if(!CollectionUtils.isEmpty(images)){
               //把图片地址集合转换成sku图片实体类集合,然后批量保存到sku图片表
                this.skuImagesService.saveBatch(images.stream().map(image ->{
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setSort(0);
                    //如果当前图片地址和默认图片地址一样那就是默认图片，
                    //如果不一样就去第一张为默认图片
                    skuImagesEntity.setDefaultStatus(StringUtils.equals(skuVO.getDefaultImage(),image) ? 1 :0);
                    return skuImagesEntity;
                }).collect(Collectors.toList()));
            }
            /**
             * 2.3、保存营销属性pms_sku_attr_value
             */
            List<SkuAttrValueEntity> saleAttrs = skuVO.getSaleAttrs();
           //页面上已经传了attr_id、attr_name、attr_value，只有sku_id需要我们设置
            saleAttrs.forEach(skuAttrValueEntity -> {
                skuAttrValueEntity.setSkuId(skuId);
            });
            this.skuAttrValueService.saveBatch(saleAttrs);
            /**
             * 3、保存营销信息(skuId)
             */
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuVO,skuSaleVo);
            //SkuVo继承了SkuEntity,id属性名不符，这里需要手动设置skuId
            skuSaleVo.setSkuId(skuId);
            this.smsClient.saleSkuSales(skuSaleVo);
        });
    }

    private void saveBaseAttrs(SpuVo spu, Long spuId) {
        List<SpuAttrValueVo> baseAttrs = spu.getBaseAttrs();
        //判断非空保存
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            //批量保存
            this.spuAttrValueService.saveBatch( //把Vo集合装换成实体类集合
                    baseAttrs.stream().map(spuAttrValueVo -> {
                        SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                        //把spuAttrValueVo属性拷贝给spuAttrValueEntity
                        BeanUtils.copyProperties(spuAttrValueVo, spuAttrValueEntity);
                        spuAttrValueEntity.setSpuId(spuId);
                        return spuAttrValueEntity;
                    }).collect(Collectors.toList()));
        }
    }

    private void saveSpuDesc(SpuVo spu, Long spuId) {
        //获取decript对应的就是spu_images
        List<String> spuImages = spu.getSpuImages();
        //判断图片信息是否为空，如果为空不需要描述信息
        if(!CollectionUtils.isEmpty(spuImages)){
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            //保存spuId
            spuDescEntity.setSpuId(spuId);
            //保存描述,将集合转成字符串
            spuDescEntity.setDecript(StringUtils.join(spuImages,","));
            this.spuDescMapper.insert(spuDescEntity);
        }
    }

    private Long saveSpu(SpuVo spu) {
        //设置新增时间
        spu.setCreateTime(new Date());
        //设置更新时间
        spu.setUpdateTime(spu.getCreateTime());
        this.save(spu);
        //获取spuId
        Long spuId = spu.getId();
        return spuId;
    }
}