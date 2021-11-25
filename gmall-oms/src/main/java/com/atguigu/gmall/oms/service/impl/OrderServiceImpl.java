package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.mapper.OrderItemMapper;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVO;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements OrderService {

    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private GmallPmsClient pmsClient;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<OrderEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<OrderEntity>()
        );

        return new PageResultVo(page);
    }

    /**
     * 保存订单
     * @param orderSubmitVO
     * @param userId
     * @return
     */
    @Transactional
    @Override
    public void saveOrder(OrderSubmitVO orderSubmitVO, Long userId) {
        // 保存订单
        OrderEntity orderEntity = new OrderEntity();
//        BeanUtils.copyProperties(orderSubmitVO,orderEntity);
        orderEntity.setUserId(userId);
        orderEntity.setOrderSn(orderSubmitVO.getOrderToken());
        orderEntity.setCreateTime(new Date());
        orderEntity.setTotalAmount(orderSubmitVO.getTotalPrice());
        orderEntity.setPayAmount(orderSubmitVO.getTotalPrice());
        orderEntity.setIntegrationAmount(new BigDecimal(orderSubmitVO.getBounds() / 100));
        orderEntity.setPayType(orderSubmitVO.getPayType());
        orderEntity.setSourceType(0);//订单来源
        orderEntity.setStatus(0);
        orderEntity.setDeliveryCompany(orderSubmitVO.getDeliveryCompany());//物流公司

        //物流地址
        UserAddressEntity address = orderSubmitVO.getAddress();
        if(address != null){
          orderEntity.setReceiverRegion(address.getRegion());
          orderEntity.setReceiverProvince(address.getProvince());
          orderEntity.setReceiverPostCode(address.getPostCode());
          orderEntity.setReceiverPhone(address.getPhone());
          orderEntity.setReceiverName(address.getName());
          orderEntity.setReceiverCity(address.getCity());
          orderEntity.setReceiverAddress(address.getAddress());
        }

        orderEntity.setDeleteStatus(0);
        orderEntity.setUseIntegration(orderSubmitVO.getBounds());

        this.save(orderEntity);
        Long orderId = orderEntity.getId();

        // 保存订单详情
        List<OrderItemVo> orderItems = orderSubmitVO.getItems();
        if(!CollectionUtils.isEmpty(orderItems)) {

            orderItems.forEach(item ->{
                OrderItemEntity orderItemEntity = new OrderItemEntity();

                // 订单信息
                orderItemEntity.setOrderId(orderId);
                orderItemEntity.setOrderSn(orderSubmitVO.getOrderToken());
                orderItemEntity.setSkuQuantity(item.getCount().intValue());


                //根据skuId查询sku
                ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
                SkuEntity skuEntity = skuEntityResponseVo.getData();
                if(skuEntity != null){
                    orderItemEntity.setSkuId(skuEntity.getId());
                    orderItemEntity.setSkuPrice(skuEntity.getPrice());
                    orderItemEntity.setSkuName(skuEntity.getName());
                    orderItemEntity.setSkuPic(skuEntity.getDefaultImage());
                    orderItemEntity.setCategoryId(skuEntity.getCategoryId());

                    //spu相关信息
                    ResponseVo<SpuEntity> spuEntityResponseVo = this.pmsClient.querySpuById(skuEntity.getSpuId());
                    SpuEntity spuEntity = spuEntityResponseVo.getData();
                    orderItemEntity.setSpuId(spuEntity.getId());
                    orderItemEntity.setSpuName(spuEntity.getName());

                    //海报信息
                    ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(spuEntity.getId());
                    SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
                    orderItemEntity.setSpuPic(spuDescEntity.getDecript());

                    //品牌信息
                    ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
                    BrandEntity brandEntity = brandEntityResponseVo.getData();
                    orderItemEntity.setSpuBrand(brandEntity.getName());

                    //销售属性
                    ResponseVo<List<SkuAttrValueEntity>> responseVo = this.pmsClient.querySkuAttrValuesBySkuId(skuEntity.getId());
                    List<SkuAttrValueEntity> skuAttrValueEntities = responseVo.getData();
                    orderItemEntity.setSkuAttrsVals(JSON.toJSONString(skuAttrValueEntities));

                    orderItemEntity.setRealAmount(skuEntity.getPrice());
                    //TODO：查询赠送积分信息
                }
                this.orderItemMapper.insert(orderItemEntity);
            });
        }
//        try {
//            TimeUnit.SECONDS.sleep(3);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        //发送延时消息定时关单
//        this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE","order.dead",orderSubmitVO.getOrderToken());
    }
}