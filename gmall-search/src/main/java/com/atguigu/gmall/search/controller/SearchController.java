package com.atguigu.gmall.search.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import com.atguigu.gmall.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("search")
    public String search(SearchParamVo paramVo, Model model){

        SearchResponseVo responseVo = this.searchService.search(paramVo);
        //响应数据
        model.addAttribute("response",responseVo);
        //查询条件
        model.addAttribute("searchParam",paramVo);

        return "search";
    }
}
