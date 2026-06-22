package com.example.ai.fallback;

import com.example.ai.client.OrderClient;
import com.example.common.domain.AjaxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 订单服务 Feign 客户端降级工厂
 * <p>
 * 当订单服务（order-service）不可用或调用失败时，Sentinel 会触发此降级工厂，
 * 为 {@link OrderClient} 中的所有方法提供备选响应，
 * 避免因订单服务故障导致 AI 智能体工具调用异常。
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-06-19
 */
@Component
public class OrderClientFallbackFactory implements FallbackFactory<OrderClient> {

    private static final Logger log = LoggerFactory.getLogger(OrderClientFallbackFactory.class);

    /**
     * 创建降级后的 OrderClient 代理
     * <p>
     * 当远程调用失败时，会执行此方法返回的匿名内部类中的逻辑。
     * 此处打印异常日志，返回错误响应（由调用方 ToolProvider 封装后返回给大模型）。
     * </p>
     *
     * @param cause 远程调用失败的原因（异常信息）
     * @return 降级后的 OrderClient 实例
     */
    @Override
    public OrderClient create(Throwable cause) {
        return new OrderClient() {
            /**
             * 根据订单号查询订单详情 - 降级
             *
             * @param orderNo 订单号
             * @return 错误响应（业务码500）
             */
            @Override
            public AjaxResult orderInfoByOrderNo(String orderNo) {
                log.error("[Fallback] 订单服务不可用, orderNo={}, cause={}", orderNo, cause.getMessage());
                return AjaxResult.error("订单服务不可用，请稍后重试");
            }
        };
    }
}
