package com.example.userservice.controller;

import com.example.common.domain.AjaxResult;
import com.example.common.exception.BusinessException;
import com.example.userservice.client.OrderClient;
import com.example.userservice.dto.OrderDTO;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.entity.User;
import com.example.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户服务 REST 控制器
 * <p>
 * 提供用户注册、登录、信息查询、订单列表查询等 API 接口
 * 部分接口（如等单列表）会通过 Feign 调用订单服务，并利用 Redis 缓存结果
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private OrderClient orderClient;

    /**
     * 根据用户 ID 查询用户信息（仅用于测试）
     * @param id 用户 ID
     * @return 用户实体
     */
    @GetMapping("/testId/{id}")
    public User getUserById(@PathVariable Long id) {
        System.out.println("============");
        System.out.println(id);
        System.out.println("============");
        return userService.getUserById(id);
    }

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户实体
     */
    @GetMapping("/username/{username}")
    public User getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username);
    }

    /**
     * 用户注册
     *
     * @param user 用户信息（ JSON 格式）
     * @return 注册结果响应
     */
    @RequestMapping("/register")
    public ResponseEntity<AjaxResult> registerUser(@RequestBody User user) {
        try {
            userService.register(user);
            return ResponseEntity.ok(AjaxResult.success("注册成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(AjaxResult.error(e.getMessage()));
        }
    }

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码（明文）
     * @return 登录成功是返回 JWT Token，失败返回错误信息
     */
    @PostMapping("/login")
    public ResponseEntity<AjaxResult> login(@RequestParam String username, @RequestParam String password) {
        try {
            String token = userService.login(username, password);
            return ResponseEntity.ok(AjaxResult.success(token));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(AjaxResult.error(e.getMessage()));
        }
    }

    /**
     * 获取用户的订单列表（带缓存）
     * <p>
     * 通过调用userService.getOrder 实现，内部会先查 Redis 缓存，未命中则调用订单服务
     * </p>
     * @param userId 用户 ID
     * @return 订单列表响应
     */
    @GetMapping("/getOrder")
    public ResponseEntity<AjaxResult> getOrder(@RequestParam Long userId) {
        try {
            List<OrderDTO> orderDTOS = userService.getOrders(userId);
            return ResponseEntity.ok(AjaxResult.success(orderDTOS));
        }catch (BusinessException e) {
            return ResponseEntity.status(400).body(AjaxResult.error(e.getMessage()));
        }
    }

    /**
     * 测试配置中心动态刷新接口（用于演示 Nacos 配置中心）
     *
     * @param jwtSecret 从配置中心获取的 JWT 密钥
     * @return JWT 密钥字符串
     */
    @GetMapping("/config/test")
    public String getJwtSecret(@Value("${jwt.secret}") String jwtSecret) {
        return jwtSecret;
    }

    /**
     * 根据用户 ID 获取订单列表（直接调用订单服务 Feign 客户端）
     * <p>
     * 该接口不经过 user-service 的缓存层，直接透传订单服务的结果
     * </p>
     *
     * @param userId 用户 ID
     * @return 订单列表响应
     */
    @GetMapping("/{userId}/orders")
    public ResponseEntity<AjaxResult> getUserOrders(@PathVariable Long userId) {
        List<OrderDTO> orders = orderClient.getOrdersByUserId(userId);
        return ResponseEntity.ok(AjaxResult.success(orders));
    }

    /**
     * 批量查询用户信息（用于商品评论展示）
     * <p>
     * 根据用户ID列表批量获取用户信息，用于评论列表中展示评论者用户名
     * </p>
     *
     * @param userList 用户ID列表
     * @return 用户信息列表
     */
    @PostMapping("/reviewUserInfo")
    public ResponseEntity<AjaxResult> getReviewUserInfo(@RequestBody List<Long> userList) {
        try {
            List<UserDTO> userDTOS = userService.reviewUserInfo(userList);
            return ResponseEntity.ok(AjaxResult.success(userDTOS));
        }catch (BusinessException e){
            return ResponseEntity.status(400).body(AjaxResult.error(e.getMessage()));
        }

    }
}
