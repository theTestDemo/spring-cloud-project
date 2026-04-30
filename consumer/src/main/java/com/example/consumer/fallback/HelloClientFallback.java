package com.example.consumer.fallback;

import com.example.consumer.client.HelloClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class HelloClientFallback implements HelloClient {
    @PostConstruct
    public void init() {
        System.out.println("HelloClientFallback bean 初始化成功");
    }
    @Override
    public String hello(String name) {
        return "服务降级：nacos-discovery-service 不可用";
    }
}