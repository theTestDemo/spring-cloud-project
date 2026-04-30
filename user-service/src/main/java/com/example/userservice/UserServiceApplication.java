package com.example.userservice;

import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.example.userservice.mapper")
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
    @Bean
    public CommandLineRunner printRocketMQConfig(RocketMQProperties rocketMQProperties) {
        return args -> {
            System.out.println("RocketMQ NameServer: " + rocketMQProperties.getNameServer());
        };
    }
}