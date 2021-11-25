package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.apache.tomcat.jni.Lock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.wms.mapper.WareSkuMapper;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuMapper, WareSkuEntity> implements WareSkuService {

    @Autowired
    private WareSkuMapper wareSkuMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    private static final String LOCK_PREFIX = "stock:lock:";
    private static final String KEY_PREFIX = "stock:info:";

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<WareSkuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public List<SkuLockVo> checkLock(List<SkuLockVo> lockVos, String orderToken) {
        if (CollectionUtils.isEmpty(lockVos)){
            return null;
        }
        lockVos.forEach(lockVo -> {
            // 每一个商品验库存并锁库存
            this.checkAndLock(lockVo);
        });
        // 如果有一个商品锁定失败了，所有已经成功锁定的商品要解库存
        if(lockVos.stream().anyMatch(skuLockVo -> !skuLockVo.getLock())){
            lockVos.stream().filter(SkuLockVo::getLock).collect(Collectors.toList()).forEach(lockVo ->{
                this.wareSkuMapper.unlockStock(lockVo.getWareSkuId(),lockVo.getCount());
            });
            //如果锁定失败，返回锁定信息
            return lockVos;
        }
        // 把库存的锁定信息保存到redis中，以方便将来解锁库存
        // 如果都锁定成功，应该把锁定状态保存到redis中（orderToken作为Key,以锁定信息作为value）
        this.redisTemplate.opsForValue().set(KEY_PREFIX + orderToken, JSON.toJSONString(lockVos));

//        this.rabbitTemplate.convertAndSend("ORDER-EXCHANGE","stock.ttl",orderToken);
        // 如果都锁定成功，不需要展示锁定情况
        return null;
    }
    public void checkAndLock(SkuLockVo skuLockVo){
        //锁当前库存
        RLock lock = this.redissonClient.getLock( LOCK_PREFIX+ skuLockVo.getSkuId());
        lock.lock();//加锁
        try {
            //验库存
            List<WareSkuEntity> wareSkuEntities = this.wareSkuMapper.checkStock(skuLockVo.getSkuId(),skuLockVo.getCount());
            if(CollectionUtils.isEmpty(wareSkuEntities)){
                skuLockVo.setLock(false);//库存不足，锁定库存失败
    //            Lock.unlock(); // 程序返回之前，一定要释放锁
                return;
            }
            // 锁库存。一般会根据运输距离，就近调配。这里就锁定第一个仓库的库存
            WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);
            if(this.wareSkuMapper.lockStock(wareSkuEntity.getId(),skuLockVo.getCount()) == 1){
                skuLockVo.setLock(true);//锁定库存
                skuLockVo.setWareSkuId(wareSkuEntity.getId());
            }else{
                skuLockVo.setLock(false);//库存锁定失败
            }
        } finally {
            lock.unlock();
        }
    }
}