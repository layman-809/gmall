package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {
    /**
     * 分页查询spu
     * @param paramVo
     * @return
     */
    @PostMapping("pms/spu/json")
    public ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo paramVo);

    /**
     * 根据spuId查询库存
     * @param spuId
     * @return
     */
    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> queryBySpuId(@PathVariable("spuId") Long spuId);

    /**
     * 根据Id查询品牌
     * @param id
     * @return
     */
    @GetMapping("pms/brand/{id}")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    /**
     * 根据Id查询分类
     * @param id
     * @return
     */
    @GetMapping("pms/category/{id}")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    /**
     * 根据cid skuId查询销售类型的检索类型的规格参数和值
     * @param cid
     * @param skuId
     * @return
     */
    @GetMapping("pms/skuattrvalue/category/{cid}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValueByCidAndSkuId(@PathVariable("cid") Long cid,
                                                                                  @RequestParam("skuId") Long skuId);

    /**
     *  根据cid spuId查询基本类型的检索类型的规格参数和值
     * @param cid
     * @param spuId
     * @return
     */
    @GetMapping("pms/spuattrvalue/category/{cid}")
    public ResponseVo<List<SpuAttrValueEntity>> querySearchAttrValueByCidAndSpuId(@PathVariable("cid") Long cid,
                                                                                  @RequestParam("spuId") Long spuId);
}
