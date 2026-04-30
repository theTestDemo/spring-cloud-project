package com.example.userservice.client;

import com.example.userservice.dto.OrderDTO;
import com.example.userservice.fallback.OrderClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

/**
 * 用户服务 Feign 客户端
 * <p>
 * 用于订单服务调用用户服务的接口，查询用户信息
 * 配置了 Sentinel 熔断降级工厂 {@link OrderClientFallbackFactory}
 * 当用户服务不可用时，降级逻辑返回null
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-17
 */
@FeignClient(name = "order-service", fallbackFactory = OrderClientFallbackFactory.class)
public interface OrderClient {
    @GetMapping("/order/user/{userId}")
    List<OrderDTO> getOrdersByUserId(@PathVariable("userId") Long userId);
}