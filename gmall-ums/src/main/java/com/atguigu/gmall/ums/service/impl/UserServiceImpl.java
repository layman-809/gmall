package com.atguigu.gmall.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;

import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        switch (type){
            case 1: wrapper.eq("username", data); break;
            case 2: wrapper.eq("phone", data); break;
            case 3: wrapper.eq("email" ,data); break;
            default:
            return null;
        }
        //返回0表示没有该用户可以进行注册
        return this.count(wrapper) == 0;
    }

    @Override
    public void register(UserEntity userEntity, String code) {
        //TODO：1.验证短信验证码，redis中的code和code进行比较
        //2.生成盐
        String salt = StringUtils.substring(UUID.randomUUID().toString(), 0, 6);
        userEntity.setSalt(salt);//保存到user对象中
        //3.加盐加密
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword() + salt));
        //4.注册用户
        userEntity.setLevelId(1l);//会员等级
        userEntity.setNickname(userEntity.getUsername());//默认用户名称
        userEntity.setSourceType(1);//使用那种方式访问 1表示web端
        userEntity.setIntegration(1000);//默认的购物积分
        userEntity.setGrowth(1000);//默认的成长积分
        userEntity.setStatus(1);//用户状态 1表示活跃 0表示被禁言
        userEntity.setCreateTime(new Date());//注册时间
        //TODO:5.删除redis中的验证码
        this.save(userEntity);
    }

    @Override
    public UserEntity queryUser(String loginName, String password) {
        //1.根据登录名查询用户
        List<UserEntity> userEntities = this.list(new QueryWrapper<UserEntity>()
                .eq("username", loginName).or()
                .eq("phone", loginName).or()
                .eq("email", loginName));
        //2.判断用户列表是否为空
        if(CollectionUtils.isEmpty(userEntities)){
            throw new RuntimeException("用户名或密码不合法！");
        }
        //3.遍历用户列表获取盐，对用户输入明文密码加盐加密比较
        for (UserEntity userEntity : userEntities){
            if(StringUtils.equals(userEntity.getPassword(),DigestUtils.md5Hex(password + userEntity.getSalt()))){
                return userEntity;
            }
        }
        throw  new RuntimeException("用户名或密码不合法！");
    }

}