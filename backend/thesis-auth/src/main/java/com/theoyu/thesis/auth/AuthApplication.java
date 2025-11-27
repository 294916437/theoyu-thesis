package com.theoyu.thesis.auth;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.theoyu.thesis.user.api")// 扫描USER API，用于发现OpenFeign客户端
@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(com.theoyu.thesis.auth.AuthApplication.class, args);
    }
}
