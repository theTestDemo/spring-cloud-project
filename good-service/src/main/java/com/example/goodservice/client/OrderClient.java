package com.example.goodservice.client;

import com.example.common.domain.AjaxResult;
import com.example.goodservice.fallback.OrderClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 订单服务 Feign 客户端
 * <p>
 * 用于调用 order-service 的接口，支持服务熔断降级
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
@FeignClient(name = "order-service",fallbackFactory = OrderClientFallbackFactory.class)
public interface OrderClient {
    /**
     * 根据订单号查询订单信息（用于评论验证）
     *
     * @param orderNo 订单号
     * @return 订单信息结果
     */
    @GetMapping("order/orderReview")
    public AjaxResult orderReview(@RequestParam("orderNo") String orderNo);
}
