package com.atguigu.gmall.cart.interceptor;

import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    // 声明线程的局部变量
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<UserInfo>();

    /**
     * 前置方法
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取登录头信息
        // 可以从cookie中获取token和userKey，不管有没有登录都要获取userKey，这里直接先获取userKey
        String userKey = CookieUtils.getCookieValue(request, jwtProperties.getUserKey());
        if(StringUtils.isBlank(userKey)){
            userKey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request,response,jwtProperties.getUserKey(),userKey,jwtProperties.getExpire());
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setUserKey(userKey);

        //获取token
        String token = CookieUtils.getCookieValue(request, jwtProperties.getCookieName());
        if(StringUtils.isNotBlank(token)){
            //解析jwt
            Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
            //获取userid
            userInfo.setUserId( Long.valueOf(map.get("userId").toString()));
        }

        THREAD_LOCAL.set(userInfo);
        return true;//放行
    }

    /**
     * 封装了一个获取线程局部变量值的静态方法
     * @return
     */
    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

//    /**
//     * 后置方法
//     * @param request
//     * @param response
//     * @param handler
//     * @param modelAndView
//     * @throws Exception
//     */
//    @Override
//    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
//
//    }
//
    /**
     * 完成方法:
     *         在视图渲染完成之后执行，经常在完成方法中释放资源
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 调用删除方法，是必须选项。因为使用的是tomcat线程池，请求结束后，线程不会结束。
        // 如果不手动删除线程变量，可能会导致内存泄漏
        THREAD_LOCAL.remove();
    }
}
