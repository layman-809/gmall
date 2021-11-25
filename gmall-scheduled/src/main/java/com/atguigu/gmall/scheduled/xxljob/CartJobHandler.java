package com.atguigu.gmall.scheduled.xxljob;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.scheduled.bean.Cart;
import com.atguigu.gmall.scheduled.mapper.CartMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
public class CartJobHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private CartMapper cartMapper;

    //前缀
    private static final String KEY = "cart:async:exception";
    private static final String KEY_PREFIX = "cart:info:";

    @XxlJob("cartJobHandler")
    public ReturnT<String> executor(String param){
        BoundSetOperations<String, String> setOps = this.redisTemplate.boundSetOps(KEY);
        // 如果redis中出现异常的用户为空，则直接返回
        if(setOps.size() == 0){
            return ReturnT.SUCCESS;
        }
        // 获取第一个失败的用户
        String userId = setOps.pop();
        while (StringUtils.isNotBlank(userId)){
            //先删除
            this.cartMapper.delete(new UpdateWrapper<Cart>().eq("user_id",userId));
            //再查询该用户redis中的购物车
            BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(KEY_PREFIX + userId);
            //返回购物车集合
            List<Object> cartJSons = hashOps.values();
            // 如果该用户购物车数据为空，则直接进入下次循环
            if(CollectionUtils.isEmpty(cartJSons)){
                continue;
            }
            // 最后，如果不为空，同步到mysql数据库
            cartJSons.forEach(cartJSon ->{
                this.cartMapper.insert(JSON.parseObject(cartJSon.toString(),Cart.class));
            });

            // 下一个用户
            userId = setOps.pop();
        }
        return ReturnT.SUCCESS;
    }
}
