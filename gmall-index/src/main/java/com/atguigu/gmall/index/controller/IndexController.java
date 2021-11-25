package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class IndexController {
    @Autowired
    private IndexService indexService;

    @GetMapping({"/","/**"})
    public String toIndex(Model model){
        //查询三级分类
        List<CategoryEntity> categoryEntityList = this.indexService.queryLvl1Categories();
        model.addAttribute("categories",categoryEntityList);
        //ToDo:广告
        return "index";
    }

    @GetMapping("index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> queryLvl2CategoriesById(@PathVariable("pid") Long pid){
        List<CategoryEntity> categoryEntities = this.indexService.queryLvl2CategoriesById(pid);
        return ResponseVo.ok(categoryEntities);
    }

    //测试线程安全问题
    @GetMapping("index/test/lock")
    @ResponseBody
    public ResponseVo<Object> testLock(){
        this.indexService.testLock3();
        return ResponseVo.ok();
    }

    @GetMapping("index/test/read")
    @ResponseBody
    public ResponseVo<Object> testRead(){
        this.indexService.testRead();
        return ResponseVo.ok("读操作");
    }

    @GetMapping("index/test/write")
    @ResponseBody
    public ResponseVo<Object> testWrite(){
        this.indexService.testWrite();
        return ResponseVo.ok("写操作");
    }

    @GetMapping("index/test/latch")
    @ResponseBody
    public ResponseVo<Object> testLatch(){
        try {
            this.indexService.testLatch();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ResponseVo.ok("闭锁操作");
    }

    @GetMapping("index/test/countDown")
    @ResponseBody
    public ResponseVo<Object> testCountDown(){
        this.indexService.testCountDown();
        return ResponseVo.ok("放行");
    }

}
