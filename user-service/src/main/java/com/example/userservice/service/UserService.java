package com.example.userservice.service;

import com.example.userservice.dto.OrderDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.entity.User;

import java.util.List;

/**
 * 用户服务接口
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
public interface UserService {
    /**
     * 根据用户ID查询用户信息
     */
    User getUserById(Long id);

    /**
     * 根据用户名查询用户信息
     */
    User getUserByUsername(String username);

    /**
     * 用户注册
     */
    User register(User user);

    /**
     * 用户登录，返回JWT Token
     */
    String login(String username, String password);

    /**
     * 获取用户订单列表（带缓存）
     */
    List<OrderDTO> getOrders(Long userId);

    /**
     * 批量查询用户信息（用于商品评论展示）
     */
    List<UserDTO> reviewUserInfo(List<Long> userIds);
}
