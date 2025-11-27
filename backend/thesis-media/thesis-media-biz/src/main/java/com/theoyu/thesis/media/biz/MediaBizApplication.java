package com.theoyu.thesis.media.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.theoyu.thesis.media.biz.model.mapper")
@EnableFeignClients(basePackages = { // 扫描OSS API，用于发现OpenFeign客户端
        "com.theoyu.thesis.oss.api",
        "com.theoyu.thesis.id.generator.api",
        "com.theoyu.thesis.user.api",
})
public class MediaBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaBizApplication.class, args);
    }

}
