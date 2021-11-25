package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * 用户表
 *
 * @author layman
 * @email 1316165298@qq.com
 * @date 2021-08-28 23:48:56
 */
public interface UserService extends IService<UserEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    Boolean checkData(String data, Integer type);

    void register(UserEntity userEntity,String code);

    UserEntity queryUser(String loginName, String password);
}

