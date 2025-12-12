package com.theoyu.thesis.chat.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.theoyu.thesis.chat.biz.model.mapper")
@EnableFeignClients(basePackages = { // 扫描OSS API，用于发现OpenFeign客户端
        "com.theoyu.thesis.kv.api",
        "com.theoyu.thesis.id.generator.api",
        "com.theoyu.thesis.user.api",
})
public class ChatBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatBizApplication.class, args);
    }

}
