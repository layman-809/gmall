package com.atguigu.gmall.index.fegin;

import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("pms-service")
public interface GmallPmsClient extends GmallPmsApi {

}
