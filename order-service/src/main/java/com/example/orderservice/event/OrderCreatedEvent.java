package com.example.orderservice.event;

import org.springframework.context.ApplicationEvent;

/**
 * 订单创建成功事件
 * <p>
 * 当下单事务提交后，通过此事件传递 userId 和 orderNo，
 * 由 {@code OrderEventListener} 异步处理后续通知和统计。
 * </p>
 */
public class OrderCreatedEvent extends ApplicationEvent {

    private final String userId;
    private final String orderNo;

    /**
     * 构造函数
     *
     * @param source  事件源（一般是调用 this 的那个 Service 对象）
     * @param userId  下单用户ID
     * @param orderNo 生成的订单号
     */
    public OrderCreatedEvent(Object source, String userId, String orderNo) {
        super(source);
        this.userId = userId;
        this.orderNo = orderNo;
    }

    public String getUserId() { return userId; }
    public String getOrderNo() { return orderNo; }
}