package com.atguigu.gmall.auth.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@Data
@Slf4j
@ConfigurationProperties(prefix = "auth.jwt") //统一读取配置文件的前缀
public class JwtProperties {

        private String pubKeyPath;//配置文件中配置的公钥
        private String priKeyPath;//配置文件中配置的私钥
        private String secret;
        private String cookieName;
        private Integer expire;
        private String unick;

        private PublicKey publicKey;//公钥对象
        private PrivateKey privateKey;//私钥对象

        /**
         * 该方法在构造方法执行之后执行
         */
        @PostConstruct
        public void init(){
            try {
                //创建公钥 私钥文件用来判断是否存在
                File pubFile = new File(pubKeyPath);
                File priFile = new File(priKeyPath);
                if(!pubFile.exists() || !priFile.exists()){
                      //如果有任何一个不存在都要重新生成
                      RsaUtils.generateKey(pubKeyPath,priKeyPath,secret);
                }
                //通过公钥文件获取公钥对象
                this.publicKey= RsaUtils.getPublicKey(pubKeyPath);
                this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
}
