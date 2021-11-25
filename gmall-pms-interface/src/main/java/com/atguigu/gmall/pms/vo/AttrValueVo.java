package com.atguigu.gmall.pms.vo;

import lombok.Data;

@Data
public class AttrValueVo {

    private Long attrId;
    //规格参数的名称
    private String attrName;
    //规格参数的值
    private String attrValue;
}