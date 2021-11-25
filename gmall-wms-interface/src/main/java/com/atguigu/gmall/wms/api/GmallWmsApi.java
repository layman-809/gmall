package com.atguigu.gmall.wms.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface GmallWmsApi {
    /**
     * 根据skuId查询库存
     * @param skuId
     * @return
     */
    @GetMapping("wms/waresku/sku/{skuId}")
    public ResponseVo<List<WareSkuEntity>> querySkuById(@PathVariable("skuId") Long skuId);

    //验库存锁库存
    @PostMapping("wms/waresku/check/lock/{orderToken}")
    public ResponseVo<List<SkuLockVo>> checkLock(@RequestBody List<SkuLockVo> lockVos, @PathVariable("orderToken") String orderToken);
}
