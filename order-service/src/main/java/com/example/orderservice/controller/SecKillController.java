package com.example.orderservice.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.example.common.domain.AjaxResult;
import com.example.orderservice.service.SecKillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 秒杀控制器
 * <p>
 * 提供秒杀接口，使用 Sentinel 进行限流保护，防止突发流量压垮系统
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-27
 */
@RestController
@RequestMapping("/order")
public class SecKillController {
    @Autowired
    private SecKillService secKillService;

    /**
     * 秒杀商品
     * <p>
     * 业务流程：
     * 1、从请求头 X-User-Id 获取用户ID（由网关解析 JWT 后注入，不可伪造）
     * 2、调用秒杀服务执行秒杀逻辑（防重、口库存、异步下单）
     * 3、返回订单号
     * </p>
     * <p>
     * 限流：通过 @SentinelResource 配置资源名 "secKill"，限流阈值在 SentinelConfig 中设置（QPS=5），
     * 触发后调用 secKillBlockHandler 返回”系统繁忙“
     * </p>
     *
     * @param userId 用户ID（从请求头自动注入）
     * @param goodId 秒杀商品ID
     * @return 订单号（成功时返回订单号字符串）
     */
    @GetMapping("secKill")
    @SentinelResource(value = "secKill", blockHandler = "secKillBlockHandler")
    public ResponseEntity<AjaxResult> secKill(@RequestHeader("X-User-Id") Long userId,
                                              @RequestParam Long goodId) {
        String orderNo = secKillService.SecKill(userId, goodId);
        return ResponseEntity.ok(AjaxResult.success(orderNo));
    }

    /**
     * 秒杀接口限流降级方法
     * <p>
     * 当秒杀接口触发  Sentinel 限流规则（QPS 超过阈值）时，返回友好提示
     * </p>
     * @param userId 用户ID
     * @param goodId 商品ID
     * @return 限流响应（HTTP 400）
     */
    public ResponseEntity<AjaxResult> secKillBlockHandler(Long userId, Long goodId) {
        return ResponseEntity.status(429).body(AjaxResult.error("系统繁忙，请稍后重试"));
    }
}
