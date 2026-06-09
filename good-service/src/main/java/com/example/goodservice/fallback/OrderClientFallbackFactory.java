package com.example.goodservice.fallback;

import com.example.common.domain.AjaxResult;
import com.example.goodservice.client.OrderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * OrderClient 熔断降级工厂
 * <p>
 * 当 order-service 服务不可用时，返回降级响应，避免级联故障
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
@Component
public class OrderClientFallbackFactory implements FallbackFactory<OrderClient> {
    private static final Logger log = LoggerFactory.getLogger(OrderClientFallbackFactory.class);

    @Override
    public OrderClient create(Throwable cause) {
        log.error("order-service 调用失败: {}", cause.getMessage());
        return new OrderClient() {
            /**
             * 订单查询降级方法
             * <p>
             * 服务不可用时返回错误提示，调用方应处理该降级响应
             * </p>
             *
             * @param orderNo 订单号
             * @return 降级响应（错误信息）
             */
            @Override
            public AjaxResult orderReview(String orderNo) {
                return AjaxResult.error("获取订单信息失败");
            }
        };
    }
}
