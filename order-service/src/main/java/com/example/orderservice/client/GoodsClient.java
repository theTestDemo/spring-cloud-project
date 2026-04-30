package com.example.orderservice.client;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.example.common.domain.AjaxResult;
import com.example.orderservice.fallback.GoodsInfoFallbackFactory;
import jdk.internal.org.objectweb.asm.tree.analysis.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 商品服务 Feign 客户端
 * <p>
 * 用于订单服务调用商品服务（good-service）的接口，实现商品信息查询、库存扣减和恢复等操作
 * 配置了 Sentinel 熔断降级工厂{@link GoodsInfoFallbackFactory},当商品服务不可用时提供降级响应
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-22
 */
@FeignClient(name = "good-service",fallbackFactory = GoodsInfoFallbackFactory.class)
public interface GoodsClient {
    /**
     * 根据商品 ID 查询商品信息
     *
     * @param id 商品ID
     * @return 商品信息（封装在 AjaxResult 中）
     */
    @GetMapping("good/goodInfo")
    public AjaxResult goodInfoById(@RequestParam("id") Long id);

    /**
     * 扣减商品库存
     *
     * @param id        商品ID
     * @param quantity  扣减数量
     * @return 操作结果（成功或失败信息）
     */
    @PostMapping("good/reduceStock")
    public AjaxResult reduceStock(@RequestParam("id") Long id, @RequestParam("quantity") Integer quantity);

    /**
     * 恢复商品库存（用于订单超时取消等场景）
     *
     * @param id        商品ID
     * @param quantity  恢复数量
     * @return 操作结果
     */
    @PostMapping("good/increaseStock")
    public AjaxResult increaseStock(@RequestParam("id") Long id, @RequestParam("quantity") Long quantity);
}
