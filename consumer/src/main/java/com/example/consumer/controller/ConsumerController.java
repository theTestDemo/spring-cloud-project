package com.example.consumer.controller;

import com.example.consumer.client.HelloClient;
import com.example.consumer.client.UserClient;
import com.example.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsumerController {
    @Autowired
    private HelloClient helloClient;
    @Autowired
    private UserClient userClient;

    @GetMapping("/call")
    public String call(@RequestParam String name) {
        return helloClient.hello(name);
    }
    @GetMapping("/user/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        return userClient.getUserById(id);
    }
}