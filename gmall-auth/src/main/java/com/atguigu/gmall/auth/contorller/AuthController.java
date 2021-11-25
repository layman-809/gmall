package com.atguigu.gmall.auth.contorller;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class AuthController {
    @Autowired
    private AuthService authService;

    @GetMapping("toLogin.html")//打开登录页面
    public String toLogin(@RequestParam("returnUrl")String returnUrl, Model model){

        // 把登录前的页面地址，记录到登录页面，以备将来登录成功，回到登录前的页面
        model.addAttribute("returnUrl", returnUrl);
        return "login";
    }
    //重定向登录方法
    @PostMapping("login")
    public String login(@RequestParam("loginName") String loginName,
                        @RequestParam("password") String password,
                        @RequestParam(value = "returnUrl",defaultValue = "http://gmall.com") String returnUrl,
                        HttpServletRequest request, HttpServletResponse response){

        this.authService.login(loginName,password,returnUrl,request,response);
        //登录成功重定向到登录前页面
        return "redirect:" + returnUrl;
    }
}
