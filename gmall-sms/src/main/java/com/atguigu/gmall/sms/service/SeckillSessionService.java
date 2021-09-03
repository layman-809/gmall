package com.atguigu.gmall.sms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.sms.entity.SeckillSessionEntity;

import java.util.Map;

/**
 * 秒杀活动场次
 *
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 23:03:41
 */
public interface SeckillSessionService extends IService<SeckillSessionEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

