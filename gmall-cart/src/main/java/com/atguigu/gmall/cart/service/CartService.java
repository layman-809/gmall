package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;

import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;

import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private CartAsyncService cartAsyncService;

    //购物车前缀
    private static final String KEY_PREFIX = "cart:info:";
    private static final String PRICE_PREFIX = "cart:price:";

    public void addCart(Cart cart) {
        //获取登录状态
        String userId = getUserId();
        //获取内层的map Map<skuId, cartJson>
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);

        //判断当前用户的购物车是否包含该商品
        String skuId = cart.getSkuId().toString();
        //新增的数量
        BigDecimal count = cart.getCount();
        if(hashOps.hasKey(skuId)){
            //更新购物车
            String cartJson = hashOps.get(skuId).toString();//已有的购物车
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(cart.getCount().add(count));
//            this.cartMapper.update(cart, new UpdateWrapper<Cart>().eq("user_id",userId).eq("sku_id",skuId));
            this.cartAsyncService.updateCartByUserIdAndSkuId(userId,cart);
        }else{
            //不包含，新增记录
            cart.setUserId(userId);
            cart.setCheck(true);
            //查询sku信息
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
           if(skuEntity == null){
                throw new CartException("你加入购物车的商品不存在！！！");
           }
           cart.setTitle(skuEntity.getTitle());
           cart.setDefaultImage(skuEntity.getDefaultImage());
           cart.setPrice(skuEntity.getPrice());

           //查询库存
            ResponseVo<List<WareSkuEntity>> listResponseVo = this.wmsClient.querySkuById(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = listResponseVo.getData();
            if(!CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
            //查询当前sku的销售属性集合
            ResponseVo<List<SkuAttrValueEntity>> responseVo = this.pmsClient.querySkuAttrValuesBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = responseVo.getData();
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntities));
            //查询营销信息
            ResponseVo<List<ItemSaleVo>> salesBySkuId = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = salesBySkuId.getData();
            cart.setSales(JSON.toJSONString(itemSaleVos));

            this.cartAsyncService.saveCart(userId,cart);

            //缓存实时价格
            this.redisTemplate.opsForValue().set(PRICE_PREFIX + skuId,skuEntity.getPrice().toString());
        }
        //把更新后的购物车写入数据库（redis mysql）
        hashOps.put(skuId,JSON.toJSONString(cart));
    }

    private String getUserId() {
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userId = userInfo.getUserKey();
        if(userInfo.getUserId() != null){
            userId = userInfo.getUserId().toString();
        }
        return userId;
    }

    public Cart queryCartByUserIdAndSkuId(Cart cart) {
        //获取登录状态
        String userId = this.getUserId();
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);

        if(hashOps.hasKey(cart.getSkuId().toString())){
            String cartJson = hashOps.get(cart.getSkuId().toString()).toString();
            //反序列化
            return JSON.parseObject(cartJson,Cart.class);
        }
        throw new CartException("新增的购物车商品不存在！！！");
    }

    @Async
    public ListenableFuture<String> executor1() {
        try {
            System.out.println("executor1方法开始执行！");
            TimeUnit.SECONDS.sleep(4);
            System.out.println("executor1方法执行结束！");
            return AsyncResult.forValue("executor1");//正常响应
        } catch (InterruptedException e) {
            e.printStackTrace();
            return AsyncResult.forExecutionException(e);//错误响应
        }

    }

    @Async
    public String executor2() {
        try {
            System.out.println("executor2方法开始执行");
            TimeUnit.SECONDS.sleep(5);
            System.out.println("executor2方法结束执行。。。");
//            int i = 1 / 0; // 制造异常
            return "executor2"; // 正常响应
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Cart> queryCarts() {

        UserInfo userInfo = LoginInterceptor.getUserInfo();
        //以userKey查询未登录的购物车
        String userKey = userInfo.getUserKey();
        //1.查询未登录的购物车
        String unloginKey = KEY_PREFIX + userKey;
        //获取了未登录的购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(unloginKey);
        //获取未登录购物车的json集合
        List<Object> cartJsons = hashOps.values();
        List<Cart> unLoginCarts = null;//未登录的购物车
        // 反序列化为cart集合
        if(!CollectionUtils.isEmpty(cartJsons)){
            unLoginCarts = cartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                //查询实时价格给购物车对象
                cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }
        //2.获取登录状态：拦截器 threadLocal 获取userId 如果userId为空 说明没有登录， 直接返回未登录的购物车
        Long userId = userInfo.getUserId();
        if(userId == null){
            return unLoginCarts;
        }
        //3.如果登录了，则合并未登录的购物车到已登录的购物车中
        String loginKey = KEY_PREFIX + userId;
        // 获取了登录状态购物车操作对象
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        // 判断是否存在未登录的购物车，有则遍历未登录的购物车合并到已登录的购物车中
        if(!CollectionUtils.isEmpty(unLoginCarts)){
            unLoginCarts.forEach(cart -> {
                String skuId = cart.getSkuId().toString();
                // 未登录购物车当前商品的数量
                BigDecimal count = cart.getCount();
                // 登录状态购物车已存在该商品，更新数量
                if(loginHashOps.hasKey(skuId)){
                    // 获取登录状态的购物车并反序列化
                    String cartJson = loginHashOps.get(skuId).toString();
                    cart = JSON.parseObject(cartJson, Cart.class);
                    //更新数量,已登录购物车数量加上未登录购物车数量
                    cart.setCount(cart.getCount().add(count));
                    //更新到redis 和 mysql
                    loginHashOps.put(skuId,JSON.toJSONString(cart));//redis
                    this.cartAsyncService.updateCartByUserIdAndSkuId(userId.toString(),cart);
                }else{
                    // 登录状态购物车不包含该记录，新增
                    cart.setUserId(userId.toString());// 用userId覆盖掉userKey
                    this.cartAsyncService.saveCart(userId.toString(),cart);

                }
                loginHashOps.put(skuId,JSON.toJSONString(cart));
            });
        }
        //4.删除登录的购物车
        this.cartAsyncService.deleteCartByUserId(userKey);
        this.redisTemplate.delete(unloginKey);

        //5.查询已登录的购物车返回给用户
        List<Object> loginCartJsons = loginHashOps.values();
        if(!CollectionUtils.isEmpty(loginCartJsons)){
            return loginCartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                //查询实时价格缓存
                cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX + cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }
        return null;
    }
    //修改商品数量
    public void updateNum(Cart cart) {
        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;
        // 获取该用户的所有购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        String skuId = cart.getSkuId().toString();
        // 判断该用户的购物车中是否包含该条信息
        if(hashOps.hasKey(skuId)){
                // 页面传递的要更新的数量
                BigDecimal count = cart.getCount();
                String cartJson = hashOps.get(skuId).toString();
                cart = JSON.parseObject(cartJson, Cart.class);
                cart.setCount(count);
                //更新到mysql 和 redis
                this.cartAsyncService.updateCartByUserIdAndSkuId(userId,cart);
                hashOps.put(skuId,JSON.toJSONString(cart));
        }
    }
    // 删除购物车
    public void deleteCart(Long skuId) {

        String userId = this.getUserId();
        String key = KEY_PREFIX + userId;
        // 获取该用户的所有购物车
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        // 判断该用户的购物车中是否包含该条信息
        if(hashOps.hasKey(skuId.toString())){
            this.cartAsyncService.deleteCartByUserIdAndSkuId(userId,skuId);
            hashOps.delete(skuId.toString());
        }
    }

    public List<Cart> queryCheckCarts(Long userId) {

       String key =  KEY_PREFIX+userId;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        List<Object> cartJsons = hashOps.values();
        if(CollectionUtils.isEmpty(cartJsons)){
            return null;
        }
        return cartJsons.stream().map(cartJson -> JSON.parseObject(cartJson.toString(),Cart.class))
                .filter(cart -> cart.getCheck()).collect(Collectors.toList());
    }
}
