package com.atguigu.gmall.index.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

@Component
public class DistributedLock {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private Timer timer;

    public Boolean tryLock(String lockName,String uuid,Integer expire){
        //加锁lua脚本
        String script = "if redis.call('exists', KEYS[1]) == 0 or redis.call('hexists', KEYS[1], ARGV[1]) == 1 " +
                "then " +
                "   redis.call('hincrby', KEYS[1], ARGV[1], 1) " +
                "   redis.call('expire', KEYS[1], ARGV[2]) " +
                "   return 1 " +
                "else " +
                "   return 0 " +
                "end";
        //execute：执行操作
        //DefaultRedisScript：使用默认的redis脚本,需要指定返回值类型，如不指定会报嵌套异常
        //动态获取保存 锁名称，uuid，过期时间
        //这里使用的StringRedisTemplate，所以所有类型必须为字符串类型
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid, expire.toString());
        if(!flag){
            //如果获取锁失败，需要重试
            try {
                //设置随眠时间，执行重试操作
                Thread.sleep(80);
                //重试
                tryLock(lockName,uuid,expire);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            //开启定时任务，定时自动续期
            this.renewExpire(lockName,uuid,expire);
        }
        //获取锁成功
        return true;
    }

    public void unLock(String lockName,String uuid){
        //解锁lua脚本
        String script = "if redis.call('hexists', KEYS[1], ARGV[1]) == 0 " +
                "then " +
                "   return nil " +
                "elseif " +
                "   redis.call('hincrby', KEYS[1], ARGV[1], -1) == 0 " +
                "then " +
                "   return redis.call('del', KEYS[1]) " +
                "else " +
                "   return 0 " +
                "end";
        //根据查询条件，这里不可以使用Boolean类型，Boolean类型只可以返回true、false，这里的返回值为null(nil)和0，所以使用Long类型
        Long flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuid);
        if(flag == null){
            //恶意释放锁，抛出异常
            throw new RuntimeException("要释放的锁不存在或不属于你！");
        }else if (flag == 1){
            //取消定时任务
            this.timer.cancel();
        }
    }
    //自动续期的定时器方法
    private void renewExpire(String lockName,String uuid,Integer expire){
        //自动续期lua脚本
        String script = "if redis.call('hexists', KEYS[1], ARGV[1]) == 1 " +
                "then " +
                "   return redis.call('expire', KEYS[1], ARGV[2]) " +
                "else " +
                "   return 0 " +
                "end";
        //设置定时器
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                redisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class),Arrays.asList(lockName),uuid,expire.toString());
            }
        },expire * 1000 / 3,expire * 1000 / 3);
    }

    //设置定时任务
    public static void main(String[] args) {
        System.out.println("定时器初始化时间："+ System.currentTimeMillis());
        //Timer：定时器
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("定时器定时任务：" + System.currentTimeMillis());
            }
        },5000,10000);//单位为毫秒
//        //newScheduledThreadPool：定时任务线程池
//        //corePoolSize：设置线程数量 3
//        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);
//        System.out.println("定时任务初始化时间：" + System.currentTimeMillis());
//        //一次性定时任务
//        scheduledExecutorService.schedule(() -> {
//            System.out.println("定时任务执行时间：" + System.currentTimeMillis());
//        },5,TimeUnit.SECONDS);//延迟5秒后执行，且只执行一次
//
//        //周期性定时任务
//        //initialDelay用来做时间校准。比如设置凌晨1点执行
//        scheduledExecutorService.scheduleAtFixedRate(() -> {
//            System.out.println("定时任务执行时间：" + System.currentTimeMillis());
//        },5,10,TimeUnit.SECONDS);//延迟5秒后执行间隔10秒后再次执行
    }
}

