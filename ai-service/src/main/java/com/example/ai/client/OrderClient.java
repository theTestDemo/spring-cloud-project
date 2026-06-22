package com.example.ai.client;

import com.example.ai.fallback.OrderClientFallbackFactory;
import com.example.common.domain.AjaxResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 订单服务 Feign 客户端
 * <p>
 * 用于 AI 智能体调用订单服务（order-service）的接口，实现订单信息查询。
 * 配置了 Sentinel 熔断降级工厂 {@link OrderClientFallbackFactory}，
 * 当订单服务不可用时提供降级响应，避免 AI 工具调用异常。
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-06-19
 */
@FeignClient(name = "order-service", fallbackFactory = OrderClientFallbackFactory.class)
public interface OrderClient {

    /**
     * 根据订单号查询订单详情
     *
     * @param orderNo 订单号
     * @return 订单信息（封装在 AjaxResult 中），降级时返回错误响应
     */
    @GetMapping("order/orderInfoByOrderNo")
    AjaxResult orderInfoByOrderNo(@RequestParam("orderNo") String orderNo);
}
