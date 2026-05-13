package com.example.orderservice.service.impl;

import com.example.orderservice.service.OrderNotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 订单异步通知服务实现
 * <p>
 * 所有 @Async 方法由 orderNotifyExecutor 线程池执行，
 * 不阻塞主流程，不参与主事务。
 * 当前包含两个异步任务：
 * <ul>
 *     <li>发送下单通知（模拟短信/邮件）</li>
 *     <li>统计用户当日下单次数（写入 Redis）</li>
 * </ul>
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-5-10
 */
@Slf4j
@Service
public class OrderNotifyServiceImpl implements OrderNotifyService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 异步发送下单通知（模拟）
     * <p>
     * 由{@code orderNotifyExecutor} 线程池的线程执行，主线程立即返回
     * 实际项目中可替换为对接短信/邮件/推送 SDK
     * </p>
     *
     * @param userId  下单用户ID
     * @param orderNo 订单号
     * @return CompletableFuture<Void> 用于任务组合或等待（当前即刻完成）
     */
    @Override
    @Async("orderNotifyExecutor")
    public CompletableFuture<Void> sendNotify(String userId, String orderNo) {
        log.info("异步线程 [{}] 发送下单通知：用户={}, 订单={}",
                Thread.currentThread().getName(), userId, orderNo);
        try {
            //模拟发送耗时（2s），实际可对接通知服务
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            //恢复中断标志，遵循线程终端最佳实践
            Thread.currentThread().interrupt();
        }
        log.info("异步线程 [{}] 通知发送完成：用户={}, 订单={}",
                Thread.currentThread().getName(), userId, orderNo);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步统计用户当日下单次数
     * <p>
     * 使用 Redis INCR 原子操作,首次创建 Key 时设置 7 天过期，避免内存泄露
     * </p>
     * @param userId 用户名ID
     * @return CompletableFuture<Void>
     */
    @Override
    @Async("orderNotifyExecutor")
    public CompletableFuture<Void> incrDayOrderCount(String userId) {
        if (stringRedisTemplate == null) {
            log.warn("StringRedisTemplate 未注入，跳过统计");
            return CompletableFuture.completedFuture(null);
        }
        String key = "order:count:" + LocalDate.now() + ":" + userId;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        //首次创建 key 时设置过期时间，防止内存无限增长
        if (Objects.equals(count, 1L)) {
            stringRedisTemplate.expire(key, 7, TimeUnit.DAYS);
        }
        log.info("异步线程 [{}] 用户当日下单次数统计：用户={}, 当前次数={}",
                Thread.currentThread().getName(), userId, count);
        return CompletableFuture.completedFuture(null);
    }
}