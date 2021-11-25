package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class GmallAuthApplicationTests {

        // 别忘了创建D:\\project\rsa目录
        private static final String pubKeyPath = "D:\\recv\\0325班\\rsa\\rsa.pub";
        private static final String priKeyPath = "D:\\recv\\0325班\\rsa\\rsa.pri";

        private PublicKey publicKey;

        private PrivateKey privateKey;

        @Test//测试生成公钥和私钥,注意需要把@BeforEach注解注释掉
        public void testRsa() throws Exception {
            RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
        }

        @BeforeEach
        public void testGetRsa() throws Exception {
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
            this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
        }

        @Test//测试testGenerateToken生成token
        public void testGenerateToken() throws Exception {
            Map<String, Object> map = new HashMap<>();
            map.put("id", "11");
            map.put("username", "liuyan");
            // 生成token
            String token = JwtUtils.generateToken(map, privateKey, 5);
            System.out.println("token = " + token);
        }

        @Test//测试解析token：
        public void testParseToken() throws Exception {
            String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MzE5Mzc3MDR9.hxkRlTXsFdinOFxSquSVVOh7Fm-1ocbwIR5BeQzOfGxqgBJpiO2fxKNNpJJxn-aG1D_QV6DPCYpoaPdWK_yKRpjiGgvODuI4JRez1KU4KS5kgqiQSY_LPiVWZ5jv80R-y3kxmHaG7GgPIjnp7257_W4rb5OSuOHOIHISrlmkS4hzeVEs_bCeObQO14c96myIsoj5kr1hiCRtJwHwYzsP07tHrnPEbuqpl8gVDqtdl4kCVA8JOwbtOv_5pIIrLeOAYoOzl-r9ImQniUHBCxiSaOcc-79jt-Bxj9LlxSjR_e68gZYeqfIeiNUpRPLixrlKAnLMXFSiuYLC2lI3Mo1Vug";

            // 解析token
            Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
            System.out.println("id: " + map.get("id"));
            System.out.println("userName: " + map.get("username"));
        }
}
