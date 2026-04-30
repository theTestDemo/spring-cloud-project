package com.example.consumer.client;


import com.example.consumer.fallback.HelloClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "nacos-discovery-service",fallback = HelloClientFallback.class)
public interface HelloClient {
    @GetMapping("/hello")
    String hello(@RequestParam("name") String name);
}
