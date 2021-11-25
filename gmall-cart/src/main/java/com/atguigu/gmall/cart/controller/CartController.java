package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import io.netty.util.concurrent.SucceededFuture;
import jdk.nashorn.internal.objects.annotations.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("check/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>> queryCheckCarts(@PathVariable("userId") Long userId){
        List<Cart> carts = this.cartService.queryCheckCarts(userId);
        return ResponseVo.ok(carts);
    }

    /**
     * 新增购物车，点击新增按钮，就会触发该方法
     * 传递参数：skuId count
     * 添加购物车成功，重定向到购物车成功页
     * @param cart
     * @return
     */
    @GetMapping
    public String addCart(Cart cart){
        if(cart == null || cart.getSkuId() == null){
            throw new RuntimeException("没有选择添加到购物车的商品信息！");
        }
        this.cartService.addCart(cart);
        //成功的请求返回重定向地址
        return "redirect:http://cart.gmall.com/addCart.html?skuId=" + cart.getSkuId() + "&count=" + cart.getCount();
    }

    /**
     * 跳转到添加成功页
     * @param model
     * @return
     */
    @GetMapping("addCart.html")
    public String queryCartByUserIdAndSkuId(Cart cart, Model model){
        BigDecimal count = cart.getCount();
        cart = this.cartService.queryCartByUserIdAndSkuId(cart);
        cart.setCount(count);
        model.addAttribute("cart",cart);
        return "addCart";//返回视图名称
    }

//    @ResponseBody
    @GetMapping("cart.html")
    public String queryCarts(Model model){
        List<Cart> carts = this.cartService.queryCarts();
        model.addAttribute("carts",carts);
        return "cart";
    }

    @GetMapping("test")
    @ResponseBody
    public String test() throws ExecutionException, InterruptedException {
        long now = System.currentTimeMillis();
        System.out.println("controller.test方法开始执行！");
        this.cartService.executor2();
        System.out.println("controller.test方法结束执行！！！" + (System.currentTimeMillis() - now));

        return "hello cart!";
    }

    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo updateNum(@RequestBody Cart cart){
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo deleteCart(@RequestParam("skuId")Long skuId){
        this.cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }
}
