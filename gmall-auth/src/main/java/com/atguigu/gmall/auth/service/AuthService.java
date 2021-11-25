package com.atguigu.gmall.auth.service;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.AuthException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {
    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private JwtProperties jwtProperties;


    public void login(String loginName,String password, String returnUrl, HttpServletRequest request, HttpServletResponse response) {
        //1.完成远程请求，获取用户信息
        UserEntity userEntity = null;
        try {
            ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUser(loginName, password);
            userEntity = userEntityResponseVo.getData();
        } catch (Exception e) {
            //2.表示密码账号可能不正确，抛出自定义异常
            throw new AuthException("用户名或密码错误！");
        }
        //3.把用户id及用户名放入载荷
        HashMap<String, Object> map = new HashMap<>();
        map.put("userId",userEntity.getId());
        map.put("userName",userEntity.getUsername());
        // 4. 为了防止jwt被别人盗取，载荷中加入用户ip地址
        String ip = IpUtils.getIpAddressAtService(request);
        map.put("ip",ip);
        // 5. 制作jwt类型的token信息 设置私钥和过期时间
        try {
            String token = JwtUtils.generateToken(map, this.jwtProperties.getPrivateKey(), this.jwtProperties.getExpire());
            // 6. 把jwt放入cookie中 请求 响应 cookName value 过期时间单位为秒（已设置过期时间单位为分钟 这里需要*60） httpOnly: true表示前端页面不可以解析cook
            CookieUtils.setCookie(request,response,this.jwtProperties.getCookieName(),token,
                    this.jwtProperties.getExpire()*60);
            // 7.用户昵称放入cookie中，方便页面展示昵称
            CookieUtils.setCookie(request,response,this.jwtProperties.getUnick(),userEntity.getNickname(),
                    this.jwtProperties.getExpire() * 60);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
