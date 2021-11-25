package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.List;

@Data
public class ItemGroupVo {
    //每个对象的Id
    private Long id;
    private String groupName;
    //组下的规格参数及值的集合
    private List<AttrValueVo> attrs;
}