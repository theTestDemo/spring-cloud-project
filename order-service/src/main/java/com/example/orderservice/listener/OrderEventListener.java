package com.example.orderservice.listener;

import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.service.OrderNotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 订单时间监听器
 * <p>
 * 使用 Spring 的 {@link TransactionalEventListener} 机制，
 * 在下单事务成功提交后异步执行通知与统计任务，避免数据不一致
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-05-10
 */
@Component
@Slf4j
public class OrderEventListener {
    @Autowired
    private OrderNotifyService orderNotifyService;

    /**
     * 处理订单创建事件
     * <p>
     * 事务提交成功（AFTER_COMMIT）后触发，由线程池 {@code orderNotifyExecutor} 异步执行
     * 如果当前没有事务上下文（例如手动调用且未开放事务），则不会执行（fallbackExecution = false）
     * </p>
     * @param event 订单创建时间，包含 userId 和 orderNo
     */
    @Async("orderNotifyExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT,fallbackExecution = false)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("监听到订单创建时间，开始异步处理：订单号={},用户={}",
                event.getOrderNo(), event.getUserId());
        //异步发送通知（模拟短信/邮件）
        orderNotifyService.sendNotify(event.getUserId(), event.getOrderNo());
        //异步统计用户当日下单次数
        orderNotifyService.incrDayOrderCount(event.getUserId());
    }
}
