package com.atguigu.gmall.pms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * spu信息介绍
 * 
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 21:05:49
 */
@Data
@TableName("pms_spu_desc")
public class SpuDescEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 商品id
	 */
	@TableId(type = IdType.INPUT)//设置手动递增id,开发人员手动设置主键的值
	private Long spuId;
	/**
	 * 商品介绍
	 */
	private String decript;

}
