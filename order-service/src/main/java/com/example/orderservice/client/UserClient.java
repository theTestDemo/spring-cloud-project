package com.example.orderservice.client;

import com.example.orderservice.dto.UserDTO;
import com.example.orderservice.fallback.UserClientFallbackFactory;
import org.apache.ibatis.annotations.Param;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务 Feign 客户端
 *
 * <p>
 * 用于订单服务调用用户服务的接口，获取用户基本信息
 * 配置了 Sentinel 熔断降级工厂 {@link UserClientFallbackFactory}
 * 当用户服务不可用时，降级逻辑返回 null（由调用方处理）
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
@FeignClient(name = "user-service",fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {
    /**
     * 根据用户 ID 查询用户信息
     *
     * @param id 用户唯一标识
     * @return 用户 DTO，若用户不存在或降级时返回null
     */
    @GetMapping("/user/testId/{id}")
    public UserDTO getUserById(@PathVariable(value = "id") Long id);
}
