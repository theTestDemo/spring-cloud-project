package com.example.ai.client;

import com.example.ai.fallback.GoodClientFallbackFactory;
import com.example.common.domain.AjaxResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 商品服务 Feign 客户端
 * <p>
 * 用于 AI 智能体调用商品服务（good-service）的接口，实现商品评论查询。
 * 配置了 Sentinel 熔断降级工厂 {@link GoodClientFallbackFactory}，
 * 当商品服务不可用时提供降级响应，避免 AI 工具调用异常。
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-06-19
 */
@FeignClient(name = "good-service", fallbackFactory = GoodClientFallbackFactory.class)
public interface GoodClient {

    /**
     * 查询商品评论列表
     *
     * @param productId 商品ID
     * @param page      页码（默认1）
     * @param pageSize  每页数量（默认10）
     * @param sort      排序字段（默认create_time）
     * @return 评论列表（封装在 AjaxResult 中），降级时返回错误响应
     */
    @PostMapping("good/review")
    AjaxResult review(@RequestParam("productId") Long productId,
                      @RequestParam(value = "page", defaultValue = "1") Integer page,
                      @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                      @RequestParam(value = "sort", defaultValue = "create_time") String sort);
}
