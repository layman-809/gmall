package com.atguigu.gmall.cart.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

@Data
@Slf4j
@ConfigurationProperties(prefix = "auth.jwt") //统一读取配置文件的前缀
public class JwtProperties {

        private String pubKeyPath;//公钥路径
        private String cookieName;//cookie
        private String userKey;
        private Integer expire; //过期时间
        private PublicKey publicKey;//公钥对象

        /**
         * 该方法在构造方法执行之后执行
         */
        @PostConstruct
        public void init(){
            try {
                this.publicKey= RsaUtils.getPublicKey(pubKeyPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
}
