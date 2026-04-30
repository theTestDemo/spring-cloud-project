package com.example.userservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.common.exception.BusinessException;
import com.example.common.util.JwtUtil;
import com.example.common.util.PasswordEncoderUtil;
import com.example.common.util.RedisUtil;
import com.example.userservice.client.OrderClient;
import com.example.userservice.dto.OrderDTO;
import com.example.userservice.entity.User;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.service.UserService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 * <p>
 * 提供用户注册、登录、信息查询、订单列表查询等功能。
 * 使用Redis缓存用户基本信息和订单列表，提高查询性能
 * </p>
 *
 * @auther 胡孟阳
 * @since 2026-04-19
 */

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private OrderClient orderClient;

    /**
     * 用户基本信息缓存 key 前缀
     */
    private static final String USER_CACHE_PREFIX = "user:";
    /**
     * 用户订单列表缓存 key 前缀
     */
    private static final String USER_ORDERS_CACHE_PREFIX = "user:orders:list:";

    /**
     * 根据用户ID查询用户信息
     * <p>
     * 先查Redis缓存，命中则直接返回；未命中则查询数据库，并将结果写入缓存，过期时间60s
     * </p>
     * @param id 用户唯一标识
     * @return 用户实体，若不存在则返回null
     */
    @Override
    public User getUserById(Long id) {
        String key = USER_CACHE_PREFIX + id;
        // 1. 从 Redis 获取缓存
        String json = redisUtil.get(key);
        if (json != null) {
            System.out.println("Cache HIT for user id: " + id);
            return JSON.parseObject(json, User.class);
        }
        // 2. 缓存未命中，查数据库
        System.out.println("Cache MISS for user id: " + id);
        User user = userMapper.findById(id);
        if (user != null) {
            // 3. 将查询结果存入 Redis，设置 60 秒过期
            redisUtil.set(key, JSON.toJSONString(user), 60, TimeUnit.SECONDS);
//            发送MQ消息（已注释，避免磁盘满影响）
//            try {
//                String msg = "用户数据已加载，用户ID: " + user.getId() + ", 用户名: " + user.getUsername();
//                rocketMQTemplate.convertAndSend("user-topic", msg);
//                System.out.println("消息已发送: " + msg);
//            } catch (Exception e) {
//                System.err.println("发送MQ消息失败: " + e.getMessage());
//                // 不影响主流程
//            }
        }

        return user;
    }

    /**
     * 根据用户名查询用户信息
     * @param username 用户名，唯一标识
     * @return 用户实体，若不存在则返回null
     */
    @Override
    public User getUserByUsername(String username) {
        return userMapper.username(username);
    }

    /**
     * 用户注册
     * <p>
     * 校验用户名是否存在，若不存在则对密码进行BCrypt加密后保存
     * </p>
     * @param user 用户信息（用户名、密码、邮箱）
     * @return 注册后的用户对象
     * @throws BusinessException 当用户名已存在时抛出业务异常
     */
    @Override
    public User register(User user) {
        User existUser = userMapper.findUsername(user.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户已存在");
        }
        user.setPassword(PasswordEncoderUtil.encode(user.getPassword()));
        userMapper.register(user);
        return user;
    }

    /**
     * 用户登录
     * <p>
     * 根据用户名查询用户，比对密码（BCrypt 匹配），成功后生成 JWT token 返回
     * </p>
     * @param username 用户名 唯一标识
     * @param password 明文密码
     * @return JWT token 字符串
     * @throws BusinessException 当用户不存在或密码错误时抛出异常
     */
    @Override
    public String login(String username, String password) {
        User user = userMapper.login(username);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!PasswordEncoderUtil.matches(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        return JwtUtil.generateToken(user.getId(),user.getUsername());
    }

    /**
     * 获取用户的订单列表（带缓存）
     * <p>
     * 先从 Redis 缓存中获取，若缓存未命中则调用订单服务 Feign 客户端查询
     * 并将结果缓存300s（5min）
     * </p>
     * @param userId 用户 id 唯一标识
     * @return 订单 DTO 列表，可能为空列表
     */
    @Override
    public List<OrderDTO> getOrders(Long userId){
        String key = USER_ORDERS_CACHE_PREFIX + userId;
        String json = redisUtil.get(key);
        if (json != null) {
            return JSON.parseArray(json, OrderDTO.class);
        }
        List<OrderDTO> userOrders = orderClient.getOrdersByUserId(userId);
        if (userOrders != null && !userOrders.isEmpty()) {
            redisUtil.set(key, JSON.toJSONString(userOrders), 300, TimeUnit.SECONDS);
        }
        return userOrders;
    }
}