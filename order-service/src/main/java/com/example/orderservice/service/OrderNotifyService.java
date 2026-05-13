package com.example.orderservice.service;

import java.util.concurrent.CompletableFuture;

/**
 * 订单异步通知服务接口
 */
public interface OrderNotifyService {

    /**
     * 异步发送下单通知
     */
    CompletableFuture<Void> sendNotify(String userId, String orderNo);

    /**
     * 异步统计用户当日下单次数
     */
    CompletableFuture<Void> incrDayOrderCount(String userId);
}