package com.atguigu.gmall.gateway.filter;

import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {

    @Autowired
    private JwtProperties jwtProperties;

   public AuthGatewayFilterFactory(){
       super(PathConfig.class);
   }
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("paths");
    }

    @Override
    public GatewayFilter apply(PathConfig config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                //获取请求对象Request ServerHttpRequest == HttpServletRequest
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();
                //1.判断当前请求的路径在不在拦截名单中。不在则直接放行
                String curPath = request.getURI().getPath();//当前请求路径
                List<String> paths = config.paths;//拦截名单
                //如果等于空则拦截所有路径，如果不为空，则判断当前请求在不在拦截名单中
                if(!CollectionUtils.isEmpty(paths) && paths.stream().allMatch(path -> curPath.startsWith(path) == false)){
                    return chain.filter(exchange);
                }
                //2.获取请求中token：同步->cookie 异步->头信息
                String token = request.getHeaders().getFirst("token");//从请求头中获取第一个token
                if(StringUtils.isBlank(token)){
                    MultiValueMap<String, HttpCookie> cookies = request.getCookies();//如果token为空从cookies中获取
                    if(!CollectionUtils.isEmpty(cookies) && cookies.containsKey(jwtProperties.getCookieName())){//不为空并包含当前cookies
                        token = cookies.getFirst(jwtProperties.getCookieName()).getValue();//从cookies中获取token对象
                    }
                }
                //3.判断token是否为空。如果为空则拦截，并重定向到登录
                if(StringUtils.isEmpty(token)){
                    response.setStatusCode(HttpStatus.SEE_OTHER);//重定向
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());//重定向到登录页面
                    return response.setComplete();
                }
                try {
                    //4.解析token，如果出现异常，则拦截 并重定向到登录
                    Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
                    //5.获取载荷中ip地址比较，不同则拦截并重定向到登陆
                    String ip = map.get("ip").toString();//载荷中的ip地址，登录用户
                    String curIp = IpUtils.getIpAddressAtGateway(request);//当前请求的ip地址
                    if(!StringUtils.equals(ip,curIp)){
                        //如果不相等重定向到登录页面
                        response.setStatusCode(HttpStatus.SEE_OTHER);//重定向
                        response.getHeaders().set(HttpHeaders.LOCATION,"http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());//重定向到登录页面
                        return response.setComplete();
                    }
                    //6.传递载荷信息给后续服务
                    //将userId转变成request对象，mutate：转变
                    request.mutate().header("userId",map.get("userId").toString()).build();
                    // 将新的request对象转变成exchange对象
                    exchange.mutate().request(request).build();
                    //7.放行
                    return chain.filter(exchange);
                } catch (Exception e) {
                    e.printStackTrace();
                    response.setStatusCode(HttpStatus.SEE_OTHER);//重定向登录
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());//重定向到登录页面
                    return response.setComplete();
                }
            }
        };
    }
    @Data
    public static class PathConfig{
        private List<String> paths;
    }
}
