package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Data//继承SpuAttrValueEntity属性字段进行扩展
public class SpuAttrValueVo extends SpuAttrValueEntity {

    public void setValueSelected(List<Object> valueSelected){
        //如果接受的集合为空，则不设置
        if(CollectionUtils.isEmpty(valueSelected)){
            return;
        }
        //拼接字符串，以逗号间隔
        this.setAttrValue(StringUtils.join(valueSelected,","));
    }
}
