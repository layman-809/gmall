package com.atguigu.gmall.index.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})//作用目标:方法
@Retention(RetentionPolicy.RUNTIME)//运行时注解：RUNTIME
//@Inherited//是否可被继承
@Documented //要不要放到文档里
public @interface GmallCache {

    /**
     * 设置缓存的前缀 默认为gmall
     */
    String prefix() default "gmall";

    /**
     * 设置缓存的过期时间默认30min
     */
    int timeout() default 30;
    /**
     * 为了防止缓存雪崩，给缓存时间添加随机值，这里可以指定随机值范围，默认10分钟
     */
    int random() default 10;
    /**
     * 为了防止缓存击穿，添加分布式锁，这里可以指定分布式锁的前缀 ，默认为lock:
     */
    String lock() default "lock:";
}
