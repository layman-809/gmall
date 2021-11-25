package com.atguigu.gmall.cart.config;

import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    /**
     * 配置拦截器方法
     * InterceptorRegistry:拦截器注册器
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //addInterceptor:指定哪个拦截器
        //addPathPatterns:拦截路径
        registry.addInterceptor(loginInterceptor).addPathPatterns("/**");
    }
}
