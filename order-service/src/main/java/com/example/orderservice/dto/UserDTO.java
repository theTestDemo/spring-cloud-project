package com.example.orderservice.dto;

import lombok.Data;

import java.util.Date;

public class UserDTO {
    @Data
    public class User {
        private Long id;
        private String username;
        private String password;
        private String email;
        private Date createdTime;
    }
}
