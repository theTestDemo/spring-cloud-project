package com.example.orderservice.fallback;

import com.example.orderservice.client.UserClient;
import com.example.orderservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 用户服务 Feign 客户端降级工厂
 * <p>
 * 当用户服务(user-service)不可用或调用失败时，Sentinel会触发此降级工厂
 * 提供备选响应：返回一个空的 UserDTO 对象（字段均为 null）
 * 避免因依赖服务故障导致订单服务异常
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    /**
     * 创建降级后的 UserClient 代理
     * <p>
     * 当远程调用失败时，会执行此方法返回的匿名内部类中的逻辑
     * 此处答应异常日志，返回一个空的 UserDTO 对象（由调用放判断字段是否为 null）
     * </p>
     * @param cause 远程他调用失败的原因（异常信息）
     * @return  降级后的 UserClient 实例
     */
    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {
            @Override
            public UserDTO getUserById(Long id) {
                System.out.println("用户信息不存在" + cause.getMessage());
                return new UserDTO();//返回空对象，调用方需自行判空
            }
        };
    }
}
