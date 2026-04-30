package com.example.userservice.mapper;

import com.example.userservice.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


@Mapper
public interface UserMapper {
    User findById(Long id);
    User username(String username);
    Integer register(User user);
    User findUsername(String username);
    User login(String username);
}
