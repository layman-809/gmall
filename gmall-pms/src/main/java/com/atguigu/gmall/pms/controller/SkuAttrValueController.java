package com.atguigu.gmall.pms.controller;

import java.util.List;

import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.PageParamVo;

/**
 * sku销售属性&值
 *
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 21:05:49
 */
@Api(tags = "sku销售属性&值 管理")
@RestController
@RequestMapping("pms/skuattrvalue")
public class SkuAttrValueController {

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    /**
     * 根据spuId查询spu下所有销售属性组合 和 skuId 映射关系
     * @param spuId
     * @return
     */
    @GetMapping("mapping/{spuId}")
    public ResponseVo<String> queryMappingBySpuId(@PathVariable("spuId") Long spuId){
        String json = this.skuAttrValueService.queryMappingBySpuId(spuId);
        return ResponseVo.ok(json);
    }
    /**
     *根据skuId查询当前sku的销售属性
     * @param skuId
     * @return
     */
    @GetMapping("sku/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>>  querySkuAttrValuesBySkuId(@PathVariable("skuId") Long skuId){
        List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueService.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id",skuId));
        return ResponseVo.ok(skuAttrValueEntities);
    }

    /**
     * 根据spuId查询spu中的attrValue销售属性
     * @return
     */
    @GetMapping("spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> querySaleAttrValueBySpuId(@PathVariable("spuId") Long spuId){
        List<SaleAttrValueVo> saleAttrValueVos = this.skuAttrValueService.querySaleAttrValueBySpuId(spuId);
        return ResponseVo.ok(saleAttrValueVos);
    }

    @GetMapping("category/{cid}")
    public ResponseVo<List<SkuAttrValueEntity>> querySearchAttrValueByCidAndSkuId(@PathVariable("cid") Long cid,
                                                                                  @RequestParam("skuId") Long skuId){
        List<SkuAttrValueEntity> skuAttrValueEntities = this.skuAttrValueService.querySearchAttrValueByCidAndSkuId(cid,skuId);
        return ResponseVo.ok(skuAttrValueEntities);
    }
    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> querySkuAttrValueByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = skuAttrValueService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<SkuAttrValueEntity> querySkuAttrValueById(@PathVariable("id") Long id){
		SkuAttrValueEntity skuAttrValue = skuAttrValueService.getById(id);

        return ResponseVo.ok(skuAttrValue);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody SkuAttrValueEntity skuAttrValue){
		skuAttrValueService.save(skuAttrValue);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody SkuAttrValueEntity skuAttrValue){
		skuAttrValueService.updateById(skuAttrValue);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		skuAttrValueService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
