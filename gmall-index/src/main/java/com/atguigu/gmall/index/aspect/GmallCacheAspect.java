package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.annotation.GmallCache;
import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MemberSignature;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RBloomFilter bloomFilter;
    /**
     * 1.必须放回Object
     * 2.必须要有ProceedingJoinPoint参数
     * 3.必须抛出Throwable类型的异常
     * 4.必须手动执行目标方法: joinPoint.proceed()
     * 通知方法：
     * 1.获取目标方法参数：joinPoint.getArgs()
     * 2.获取目标方法签名：
     */
    //切面表达式：返回值任意类型 service包下的所有类的所有方法，参数列表任意
    //"execution(* com.atguigu.gmall.index.service.*.*(..))"
    @Around("@annotation(com.atguigu.gmall.index.annotation.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint)throws Throwable {
        //getSignature：获取方法签名,返回结果集为签名对象Signature
        //顶层接口Signature：无法获取到方法的形参、方法的注解，需要使用其子接口或者实现类
        //子接口：MemberSignature的孙子接口 MethodSignature
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //通过签名获取方法对象
        Method method = signature.getMethod();
        //获取方法对象上的注解
        //GmallCache.class：表示指定要获取的注解
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
        //获取注解中的某一个
        //获取前缀
        String prefix = gmallCache.prefix();
        //获取方法的形参列表
        //将数组形参列表以逗号分割方式拼接成字符串
        String args = StringUtils.join(joinPoint.getArgs(), ",");
        //prefix:方法的前缀 args：方法形参
        String key = prefix + args;
        //为了防止缓存穿透，使用布隆过滤器
        if(!bloomFilter.contains(key)){//如果判定不存在就一定不存在
            return null;
        }
        //1、先查询缓存，命中直接返回
        String json = this.redisTemplate.opsForValue().get(key);
        //判空
        if(StringUtils.isNotBlank(json)){
            //signature.getReturnType:通过方法签名获取该方法返回值类型，或者使用方法对象也可以获取到返回值类型。
            return JSON.parseObject(json,signature.getReturnType());
        }
        //2、防止缓存击穿，添加分布式锁
        //通过方法对象获取锁的前缀
        String lock_prefix= gmallCache.lock();
        RLock lock = this.redissonClient.getLock(lock_prefix + args);
        //加锁
        lock.lock();
        try {
            //3、再查缓存，命中则直接返回
            String json2 = this.redisTemplate.opsForValue().get(key);
            //判空
            if(StringUtils.isNotBlank(json2)){
                //signature.getReturnType:通过方法签名获取该方法返回值类型，或者使用方法对象也可以获取到返回值类型。
                return JSON.parseObject(json2,signature.getReturnType());
            }
            //4、执行目标方法
            Object result = joinPoint.proceed(joinPoint.getArgs());
            //5、放入缓存，并释放分布式锁
            //获取过期时间+随机值
            //gmallCache.random():随机值范围 @gmallCache注解中有定义默认值
            int timeout = gmallCache.timeout() + new Random().nextInt(gmallCache.random());
            //TimeUnit.MINUTES:时间单位为分钟
            this.redisTemplate.opsForValue().set(key,JSON.toJSONString(result),timeout, TimeUnit.MINUTES);
            return result;
        } finally {
            //释放锁
            lock.unlock();
        }
    }

    //BloomFilter过滤器测试
//    public static void main(String[] args) {
//        //参数1：字符集编码格式（Google包下的）
//        //参数2：元素数量(key)
//        //参数3：误判率
//        BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), 20,0.3);
//        bloomFilter.put("1");
//        bloomFilter.put("2");
//        bloomFilter.put("3");
//        bloomFilter.put("4");
//        bloomFilter.put("5");
//        bloomFilter.put("6");
//        bloomFilter.put("7");
//        //mightContain:可能包含
//        System.out.println(bloomFilter.mightContain("1"));
//        System.out.println(bloomFilter.mightContain("3"));
//        System.out.println(bloomFilter.mightContain("5"));
//        System.out.println(bloomFilter.mightContain("7"));
//        System.out.println(bloomFilter.mightContain("9"));
//        System.out.println(bloomFilter.mightContain("10"));
//        System.out.println(bloomFilter.mightContain("11"));
//        System.out.println(bloomFilter.mightContain("12"));
//        System.out.println(bloomFilter.mightContain("13"));
//        System.out.println(bloomFilter.mightContain("14"));
//        System.out.println(bloomFilter.mightContain("15"));
//        System.out.println(bloomFilter.mightContain("16"));
//
//    }
}
