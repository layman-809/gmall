package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
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
     * 根据spuId查询spu
     * @param id
     * @return
     */
    @GetMapping("pms/spu/{id}")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);

    /**
     * 根据spuId查询spu的描述信息
     * @param spuId
     * @return
     */
    @GetMapping("pms/spudesc/{spuId}")
    public ResponseVo<SpuDescEntity> querySpuDescById(@PathVariable("spuId") Long spuId);

    /**
     * 根据spuId查询库存
     * @param spuId
     * @return
     */
    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> querySkusBySpuId(@PathVariable("spuId") Long spuId);

    /**
     * 根据skuId查询sku
     * @param id
     * @return
     */
    @GetMapping("pms/sku/{id}")
    public ResponseVo<SkuEntity> querySkuById(@PathVariable("id") Long id);

    /**
     * 根据skuId查询sku的图片列表
     * @param skuId
     * @return
     */
    @GetMapping("pms/skuimages/sku/{skuId}")
    public ResponseVo<List<SkuImagesEntity>> queryImagesBySkuId(@PathVariable("skuId") Long skuId);

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

    @GetMapping("pms/category/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCategory(@PathVariable("parentId") Long pid);

    @GetMapping("pms/category/withsubs/parent/{pid}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesWithSubsByPid(@PathVariable("pid") Long pid);

    /**
     * 根据三级分类的id查询一二三级分类
     * @param cid
     * @return
     */
    @GetMapping("pms/category/all/{cid}")
    public ResponseVo<List<CategoryEntity>> queryLvl123CatesByCid3(@PathVariable("cid") Long cid);

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
     * 根据spuId查询spu下所有销售属性组合 和 skuId 映射关系
     * @param spuId
     * @return
     */
    @GetMapping("pms/skuattrvalue/mapping/{spuId}")
    public ResponseVo<String> queryMappingBySpuId(@PathVariable("spuId") Long spuId);

    /**
     *根据skuId查询当前sku的销售属性
     * @param skuId
     * @return
     */
    @GetMapping("pms/skuattrvalue/sku/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>>  querySkuAttrValuesBySkuId(@PathVariable("skuId") Long skuId);
    /**
     * 根据spuId查询spu中的attrValue销售属性
     * @return
     */
    @GetMapping("pms/skuattrvalue/spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> querySaleAttrValueBySpuId(@PathVariable("spuId") Long spuId);
    /**
     *  根据cid spuId查询基本类型的检索类型的规格参数和值
     * @param cid
     * @param spuId
     * @return
     */
    @GetMapping("pms/spuattrvalue/category/{cid}")
    public ResponseVo<List<SpuAttrValueEntity>> querySearchAttrValueByCidAndSpuId(@PathVariable("cid") Long cid,
                                                                                  @RequestParam("spuId") Long spuId);

    /**
     * 根据cid、spuId、skuId查询分组及组下的规格参数和值
     * @param cid
     * @param spuId
     * @param skuId
     * @return
     */
    @GetMapping("pms/attrgroup/with/attr/value/{cid}")
    public ResponseVo<List<ItemGroupVo>> queryGroupWithAttrAndValuesByCidAndSkuIdAndSpuId(
            @PathVariable("cid") Long cid,
            @RequestParam("spuId") Long spuId,
            @RequestParam("skuId") Long skuId);
}
