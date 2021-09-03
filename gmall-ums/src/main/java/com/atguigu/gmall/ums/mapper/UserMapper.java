package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 23:48:56
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
