package com.theoyu.thesis.user.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.theoyu.thesis.user.biz.model.mapper")
@EnableFeignClients(basePackages = { // 扫描OSS API，用于发现OpenFeign客户端
        "com.theoyu.thesis.oss.api",
        "com.theoyu.thesis.id.generator.api",
})
public class UserBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserBizApplication.class, args);
    }

}
