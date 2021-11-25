package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter(){
        //初始化cors配置对象
        CorsConfiguration config = new CorsConfiguration();
        //允许的域，不要写*，否则cookie就无法使用了。
        config.addAllowedOrigin("http://manager.gmall.com");
        config.addAllowedOrigin("http://localhost:1000");
        config.addAllowedOrigin("http://gmall.com");
        config.addAllowedOrigin("http://www.gmall.com");
        //允许的头信息
        config.addAllowedHeader("*");
        //允许的请求方式
        config.addAllowedMethod("*");
        //是否允许携带cookie信息
        config.setAllowCredentials(true);
        //添加映射路径，拦截一切请求
        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        //拦截的地址和配置信息
        configurationSource.registerCorsConfiguration("/**",config);

        return new CorsWebFilter(configurationSource);
    }
}
