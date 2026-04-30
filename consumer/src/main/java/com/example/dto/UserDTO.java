package com.example.dto;


import lombok.Data;
import java.util.Date;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String password;
    private String email;
    private Date createdTime;
}

