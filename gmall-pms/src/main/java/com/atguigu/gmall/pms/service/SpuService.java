package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.SpuVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.pms.entity.SpuEntity;

import java.util.List;
import java.util.Map;

/**
 * spu信息
 *
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 21:05:49
 */
public interface SpuService extends IService<SpuEntity> {

    PageResultVo queryPage(PageParamVo paramVo);


    PageResultVo querySpuByCidAndPage(Long categoryId, PageParamVo pageParamVo);


    void bigSave(SpuVo spu);
}

