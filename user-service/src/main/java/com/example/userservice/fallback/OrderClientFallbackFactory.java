package com.example.userservice.fallback;

import com.example.userservice.client.OrderClient;
import com.example.userservice.dto.OrderDTO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

/**
 * 订单服务 Feign 客户端降级工厂
 * <p>
 * 当订单服务（order-service）不可用或调用失败时，Sentinel 会触发此降级工厂
 * 提供备选响应：返回空列表，避免因依赖服务故障导致用户服务异常
 * </p>
 *
 * @author 胡孟阳
 * @since 2024-04-20
 */
@Component
public class OrderClientFallbackFactory implements FallbackFactory<OrderClient> {
    /**
     * 降级后的 OrderClient 代理
     * <p>
     * 当远程调用失败时，会执行此方法返回的匿名内部类中的逻辑
     * 该匿名类直接实现了 OrderClient 接口，用于提供降级响应
     * 可在此处记录异常日志，并返回安全的默认值
     * </p>
     *
     * @param cause 远程调用失败的原因（异常信息）
     * @return 降级后的空列表
     */
    @Override
    public OrderClient create(Throwable cause) {
        return new OrderClient() {
            @Override
            public List<OrderDTO> getOrdersByUserId(Long userId) {
                System.out.println("调用 order-service 失败，原因：" + cause.getMessage());
                return Collections.emptyList(); // 降级返回空列表
            }
        };
    }
}