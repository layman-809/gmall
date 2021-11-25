package com.atguigu.gmall.order.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVO;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.order.vo.UserInfo;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private GmallCartClient cartClient;
    @Autowired
    private GmallWmsClient wmsClient;
    //redis序列化
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private GmallOmsClient omsClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    //前缀
    private static final String KEY_PREFIX = "order:token:";
    //线程池
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    /**
     * 订单确认页
     * 由于存在大量的远程调用，这里使用异步编排做优化
     * @return
     */
    public OrderConfirmVo confirm() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();

        // 查询送货清单
        CompletableFuture<List<Cart>> cartCompletableFuture = CompletableFuture.supplyAsync(() -> {
            ResponseVo<List<Cart>> listResponseVo = this.cartClient.queryCheckCarts(userId);
            List<Cart> carts = listResponseVo.getData();
            if (CollectionUtils.isEmpty(carts)) {
                throw new CartException("没有选中的购物车信息！");
            }
            return carts;
        }, threadPoolExecutor);

        //查新购物车商品详情
        CompletableFuture<Void> itemCompletableFuture = cartCompletableFuture.thenAcceptAsync(carts -> {
            List<OrderItemVo> items = carts.stream().map(cart -> {
                OrderItemVo orderItemVo = new OrderItemVo();
                //用户Id
                orderItemVo.setSkuId(cart.getSkuId());
                //购物车商品数量
                orderItemVo.setCount(cart.getCount().intValue());
                // 根据skuId查询sku
                CompletableFuture<Void> skuCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
                    SkuEntity skuEntity = skuEntityResponseVo.getData();
                    if (skuEntity != null) {
                        orderItemVo.setTitle(skuEntity.getTitle());//标题
                        orderItemVo.setPrice(skuEntity.getPrice());//价格
                        orderItemVo.setDefaultImage(skuEntity.getDefaultImage());//默认图片
                        orderItemVo.setWeight(new BigDecimal(skuEntity.getWeight()));//重量
                    }
                }, threadPoolExecutor);
                // 查询销售属性
                CompletableFuture<Void> saleAttrCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<List<SkuAttrValueEntity>> skuAttrValuesResponseVo = this.pmsClient.querySkuAttrValuesBySkuId(cart.getSkuId());
                    List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValuesResponseVo.getData();
                    if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                        orderItemVo.setSaleAttrs(skuAttrValueEntities);
                    }
                }, threadPoolExecutor);
                // 根据skuId查询营销信息
                CompletableFuture<Void> saleCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<List<ItemSaleVo>> itemSalesResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
                    List<ItemSaleVo> itemSaleVos = itemSalesResponseVo.getData();
                    if (!CollectionUtils.isEmpty(itemSaleVos)) {
                        orderItemVo.setSales(itemSaleVos);
                    }
                }, threadPoolExecutor);
                // 根据 skuId查询库存信息
                CompletableFuture<Void> storeCompletableFuture = CompletableFuture.runAsync(() -> {
                    ResponseVo<List<WareSkuEntity>> wareSkuResponse = this.wmsClient.querySkuById(cart.getSkuId());
                    List<WareSkuEntity> wareSkuEntities = wareSkuResponse.getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                        //anyMatch:传入一个断言型函数，对流中所有的元素进行判断，只要有一个满足条件就返回true，都不满足返回false。
                        orderItemVo.setStore(wareSkuEntities.stream()
                                .anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
                    }
                }, threadPoolExecutor);
                //allOf:等待所有线程任务结束,所有任务执行完才放行
                CompletableFuture.allOf(skuCompletableFuture, saleAttrCompletableFuture, saleCompletableFuture, storeCompletableFuture).join();
                return orderItemVo;
            }).collect(Collectors.toList());
            confirmVo.setItems(items);
        }, threadPoolExecutor);

        // 查询收货地址列表
        CompletableFuture<Void> addressCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<UserAddressEntity>> addressesResponseVo = this.umsClient.queryAddressesByUserId(userId);
            List<UserAddressEntity> userAddressEntities = addressesResponseVo.getData();
            if (!CollectionUtils.isEmpty(userAddressEntities)) {
                confirmVo.setAddresses(userAddressEntities);
            }
        }, threadPoolExecutor);

        // 查询用户的积分信息
        CompletableFuture<Void> boundsCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUserById(userId);
            UserEntity userEntity = userEntityResponseVo.getData();
            if (userEntity != null) {
                confirmVo.setBounds(userEntity.getIntegration());
            }
        }, threadPoolExecutor);

        // 防重的唯一标识
        CompletableFuture<Void> tokenCompletableFuture = CompletableFuture.runAsync(() -> {
            //IdWorker：产生唯一的id的工具类
            //getTimeId：根据时间戳生成id
            String timeId = IdWorker.getTimeId();
            this.redisTemplate.opsForValue().set(KEY_PREFIX + timeId, timeId,3, TimeUnit.HOURS);
            confirmVo.setOrderToken(timeId);
        }, threadPoolExecutor);

        CompletableFuture.allOf(itemCompletableFuture,addressCompletableFuture,boundsCompletableFuture,tokenCompletableFuture).join();

        return confirmVo;

    }

    public void submit( OrderSubmitVO submitVo) {

        // 1.防重 判断是否已提交 orderToken  到redis中判断orderToken是否存在
        String orderToken = submitVo.getOrderToken(); // 页面中的orderToken
        if (StringUtils.isBlank(orderToken)){
            throw new OrderException("非法请求！");
        }
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), orderToken);
        if (!flag) {
            throw new OrderException("请不要重复提交！");
        }

        // 2.验总价 页面中总价格 和数据库中的实时总价格比较
        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)){
            throw new OrderException("请选择要够买的商品！");
        }
        BigDecimal totalPrice = submitVo.getTotalPrice(); // 页面总价格
        BigDecimal currentTotalPrice = items.stream().map(item -> { // 实时总价格
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                return new BigDecimal(0);
            }
            return skuEntity.getPrice().multiply(new BigDecimal(item.getCount()));
        }).reduce((a, b) -> a.add(b)).get();
        if (totalPrice.compareTo(currentTotalPrice) != 0) {
            throw new OrderException("页面已过期，请刷新后重试！");
        }

        //3.验库存并立马锁库存  保证原子性
        List<SkuLockVo> skuLockVos = items.stream().map(item -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setCount(item.getCount().intValue());
            return skuLockVo;
        }).collect(Collectors.toList());
        ResponseVo<List<SkuLockVo>> wareSkuResponseVo = this.wmsClient.checkLock(skuLockVos, orderToken);
        List<SkuLockVo> lockVos = wareSkuResponseVo.getData();
        if (!CollectionUtils.isEmpty(lockVos)){
            throw new OrderException(JSON.toJSONString(lockVos));
        }

        //int i = 1/0;

        //4.创建订单
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        try {
            this.omsClient.saveOrder(submitVo, userId);
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.ttl", orderToken);
        } catch (Exception e) {
            e.printStackTrace();
            // 发送消息给oms标记为无效订单 并给wms解锁库存
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.failure", orderToken);
            throw new OrderException("服务器端错误！");
        }


        //5.删除购物车中对应的记录 异步
        Map<String, Object> msg = new HashMap<>();
        msg.put("userId", userId);
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        msg.put("skuIds", JSON.toJSONString(skuIds));
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "cart.delete", msg);
    }
}
