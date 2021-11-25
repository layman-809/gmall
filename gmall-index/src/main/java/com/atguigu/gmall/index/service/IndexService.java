package com.atguigu.gmall.index.service;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.annotation.GmallCache;
import com.atguigu.gmall.index.config.RedissonConfig;
import com.atguigu.gmall.index.fegin.GmallPmsClient;
import com.atguigu.gmall.index.utils.DistributedLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cates:";
    private static final String LOCK_PREFIX = "index:cates:lock:";

    public List<CategoryEntity> queryLvl1Categories() {

        ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategory(0l);
        return listResponseVo.getData();
    }

    @GmallCache(prefix = KEY_PREFIX , timeout = 129600 , random = 14400 ,lock = LOCK_PREFIX)
    public List<CategoryEntity> queryLvl2CategoriesById(Long pid) {
            ResponseVo<List<CategoryEntity>> listResponseVo = this.pmsClient.queryCategoriesWithSubsByPid(pid);
            List<CategoryEntity> categoryEntities = listResponseVo.getData();

            return categoryEntities;

    }

    public void testLock1() {
        String uuid = UUID.randomUUID().toString();
        //1.加锁
        //setIfAbsent如果不存在则设置
        Boolean flag = this.redisTemplate.opsForValue().setIfAbsent("lock", uuid,3,TimeUnit.SECONDS);
        if(!flag){
            //3.如果没有抢到锁，重试
            try {
                Thread.sleep(80);
                testLock1();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else {
//            this.redisTemplate.exec()
            String json = this.redisTemplate.opsForValue().get("num");
            if(StringUtils.isBlank(json)){
                this.redisTemplate.opsForValue().set("num","1");
            }
            int num = Integer.parseInt(json);
            this.redisTemplate.opsForValue().set("num",String.valueOf(++num));
            //2.解锁:防止误删，先判断是否是自己的锁，如果是自己的锁才能释放
//            if(StringUtils.equals(uuid,this.redisTemplate.opsForValue().get("lock"))){
//                this.redisTemplate.delete("lock");
//
//            }
            //使用lua脚本来防止误删锁操作，先判断是否是自己的锁，如果是自己的锁才能释放
            String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del' , KEYS[1]) else return 0 end";
            //execute：执行操作
            //DefaultRedisScript：使用默认的redis脚本,需要指定返回值类型，如不指定会报嵌套异常
            //锁为一个集合参数
            //uuid表示：ARGV[1] 有几个参数就写一个
            this.redisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class), Arrays.asList("lock"),uuid);
        }
    }

    public void testLock2() {
        //随机生成uuid
        String uuid = UUID.randomUUID().toString();
        //获取锁
        //参数1：锁名称 参数2：锁的id 参数3：过期时间
        Boolean lock = this.distributedLock.tryLock("lock", uuid, 30);
        if(lock){//ture 表示获取锁成功
            try {
                String json = this.redisTemplate.opsForValue().get("num");
                if(StringUtils.isBlank(json)){
                    this.redisTemplate.opsForValue().set("num","1");
                }
                int num = Integer.parseInt(json);
                this.redisTemplate.opsForValue().set("num",String.valueOf(++num));
                //测试
//                this.testSubLock(uuid);
                try {
                    TimeUnit.SECONDS.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } finally {
                //释放锁
                this.distributedLock.unLock("lock",uuid);
            }
        }
    }

    public void testLock3() {
        //获取锁
        RLock lock = this.redissonClient.getLock("lock");
        //加锁
        lock.lock();

        try {
            String json = this.redisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(json)) {
                this.redisTemplate.opsForValue().set("num", "1");
            }
            int num = Integer.parseInt(json);
            this.redisTemplate.opsForValue().set("num", String.valueOf(++num));
        } finally {
            //解锁
            lock.unlock();
        }
    }

    //测试可重入锁
    public void testSubLock(String uuid){
        //获取锁
        this.distributedLock.tryLock("lock",uuid,30);
        //业务操作
        System.out.println("测试可重入锁");
        //释放锁
        this.distributedLock.unLock("lock",uuid);
    }

    public void testRead() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10,TimeUnit.SECONDS);//单位秒
        // ToDO:业务代码
    }

    public void testWrite() {
        RReadWriteLock rwLock = this.redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10,TimeUnit.SECONDS);//单位秒
        // ToDO:业务代码
    }

    public void testLatch() throws InterruptedException {
        RCountDownLatch cdl = this.redissonClient.getCountDownLatch("cdl");
        cdl.trySetCount(6);//6个线程

        cdl.await();

        //TODO:业务操作
    }

    public void testCountDown() {
        RCountDownLatch cld = this.redissonClient.getCountDownLatch("cld");
        cld.countDown();
    }
}
