package com.atguigu.gmall.pms.controller;

import java.util.List;

import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 属性分组
 *
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 21:05:49
 */
@Api(tags = "属性分组 管理")
@RestController
@RequestMapping("pms/attrgroup")
public class AttrGroupController {

    @Autowired
    private AttrGroupService attrGroupService;

    /**
     * 根据cid、spuId、skuId查询分组及组下的规格参数和值
     * @param cid
     * @param spuId
     * @param skuId
     * @return
     */
    @GetMapping("with/attr/value/{cid}")
    public ResponseVo<List<ItemGroupVo>> queryGroupWithAttrAndValuesByCidAndSkuIdAndSpuId(
            @PathVariable("cid") Long cid,
            @RequestParam("spuId") Long spuId,
            @RequestParam("skuId") Long skuId){
        List<ItemGroupVo> itemGroupVos = this.attrGroupService.queryGroupWithAttrAndValuesByCidAndSkuIdAndSpuId(cid,spuId,skuId);
        return ResponseVo.ok(itemGroupVos);
    }

    /**
     * 查询分类下的组及规格参数
     */
    @GetMapping("withattrs/{catId}")
    public ResponseVo<List<GroupVo>> queryByCid(@PathVariable("catId") Long cid){
        List<GroupVo> groupVos = this.attrGroupService.queryByCid(cid);
        return ResponseVo.ok(groupVos);
    }

    /**
     * 根据三级分类id查询商品列表
     */
    @GetMapping("category/{cid}")
    public ResponseVo<List<AttrGroupEntity>> queryAttrgroup(@PathVariable("cid") Long cid){
        List<AttrGroupEntity> attrGroupEntities = this.attrGroupService.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
        return ResponseVo.ok(attrGroupEntities);
    }

    /**
     * 列表
     */
    @GetMapping
    @ApiOperation("分页查询")
    public ResponseVo<PageResultVo> queryAttrGroupByPage(PageParamVo paramVo){
        PageResultVo pageResultVo = attrGroupService.queryPage(paramVo);

        return ResponseVo.ok(pageResultVo);
    }


    /**
     * 信息
     */
    @GetMapping("{id}")
    @ApiOperation("详情查询")
    public ResponseVo<AttrGroupEntity> queryAttrGroupById(@PathVariable("id") Long id){
		AttrGroupEntity attrGroup = attrGroupService.getById(id);

        return ResponseVo.ok(attrGroup);
    }

    /**
     * 保存
     */
    @PostMapping
    @ApiOperation("保存")
    public ResponseVo<Object> save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return ResponseVo.ok();
    }

    /**
     * 修改
     */
    @PostMapping("/update")
    @ApiOperation("修改")
    public ResponseVo update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return ResponseVo.ok();
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ApiOperation("删除")
    public ResponseVo delete(@RequestBody List<Long> ids){
		attrGroupService.removeByIds(ids);

        return ResponseVo.ok();
    }

}
