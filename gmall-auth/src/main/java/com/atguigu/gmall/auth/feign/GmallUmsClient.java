package com.atguigu.gmall.auth.feign;

import com.atguigu.gmall.auth.entity.UserEntity;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.ums.api.GmallUmsApi;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("ums-service")
public interface GmallUmsClient extends GmallUmsApi {
}
