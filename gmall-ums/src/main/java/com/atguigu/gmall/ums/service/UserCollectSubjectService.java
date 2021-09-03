package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.ums.entity.UserCollectSubjectEntity;

import java.util.Map;

/**
 * 关注活动表
 *
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 23:48:56
 */
public interface UserCollectSubjectService extends IService<UserCollectSubjectEntity> {

    PageResultVo queryPage(PageParamVo paramVo);
}

