package com.atguigu.gmall.cart.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
@Slf4j
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;
    //前缀
    private static final String KEY = "cart:async:exception";

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
//        log.error("异步调用发生异常，方法：{}，参数：{}。异常信息：{}", method, objects, throwable.getMessage());
        // 把异常用户信息存入redis
        BoundSetOperations<String, String> setOps = this.redisTemplate.boundSetOps(KEY);//boundSetOps:可以去重
        if(objects != null && objects.length > 0){
            setOps.add(objects[0].toString());
        }
    }
}
