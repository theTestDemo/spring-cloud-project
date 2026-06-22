package com.example.ai.fallback;

import com.example.ai.client.GoodClient;
import com.example.common.domain.AjaxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 商品服务 Feign 客户端降级工厂
 * <p>
 * 当商品服务（good-service）不可用或调用失败时，Sentinel 会触发此降级工厂，
 * 为 {@link GoodClient} 中的所有方法提供备选响应，
 * 避免因商品服务故障导致 AI 智能体工具调用异常。
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-06-19
 */
@Component
public class GoodClientFallbackFactory implements FallbackFactory<GoodClient> {

    private static final Logger log = LoggerFactory.getLogger(GoodClientFallbackFactory.class);

    /**
     * 创建降级后的 GoodClient 代理
     * <p>
     * 当远程调用失败时，会执行此方法返回的匿名内部类中的逻辑。
     * 此处打印异常日志，返回错误响应（由调用方 ToolProvider 封装后返回给大模型）。
     * </p>
     *
     * @param cause 远程调用失败的原因（异常信息）
     * @return 降级后的 GoodClient 实例
     */
    @Override
    public GoodClient create(Throwable cause) {
        return new GoodClient() {
            /**
             * 查询商品评论列表 - 降级
             *
             * @param productId 商品ID
             * @param page      页码
             * @param pageSize  每页数量
             * @param sort      排序字段
             * @return 错误响应（业务码500）
             */
            @Override
            public AjaxResult review(Long productId, Integer page, Integer pageSize, String sort) {
                log.error("[Fallback] 商品服务不可用, productId={}, cause={}", productId, cause.getMessage());
                return AjaxResult.error("商品服务不可用，请稍后重试");
            }
        };
    }
}
