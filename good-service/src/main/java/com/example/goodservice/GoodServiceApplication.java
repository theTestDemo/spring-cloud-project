package com.example.goodservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.example.goodservice.mapper")
@EnableFeignClients
public class GoodServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GoodServiceApplication.class, args);
    }
}