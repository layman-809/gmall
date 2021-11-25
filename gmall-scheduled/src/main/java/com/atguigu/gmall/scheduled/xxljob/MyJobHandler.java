package com.atguigu.gmall.scheduled.xxljob;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;
import org.springframework.stereotype.Component;

/**
 * XxlJob开发示例（Bean模式）
 * 开发步骤：
 * 1、在Spring Bean实例中，开发Job方法，方式格式要求为 "public ReturnT<String> execute(String param)"
 * 2、为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 * 3、执行日志：需要通过 "XxlJobLogger.log()" 打印执行日志；
 */
@Component
public class MyJobHandler {

    @XxlJob("myJobHandler")
    public ReturnT<String> execute(String param) {
        XxlJobLogger.log("使用XxlJobLogger打印执行日志，O(∩_∩)O");
        System.out.println("我的任务执行了：" + param + "，线程：" + Thread.currentThread().getName());
        return ReturnT.SUCCESS;
    }
}