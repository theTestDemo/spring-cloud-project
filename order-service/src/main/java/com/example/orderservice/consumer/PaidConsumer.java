package com.example.orderservice.consumer;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 订单支付成功消息消费者
 * <p>
 * 监听订单支付成功消息，用于触发后续业务流程（如积分增加、物流通知等）
 * 当前实现仅打印日志，实际业务可扩展
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-26
 */
@Component
@RocketMQMessageListener(topic = "order-paid-topic",consumerGroup = "order-paid-group")
public class PaidConsumer implements RocketMQListener<String> {
    /**
     * 消费支付成功消息
     * <p>
     * 解析消息获取用户ID和订单号，演示异步处理。生产环境可在此发送通知、积分增加等
     * </p>
     * @param message 消息体（JSON字符串，包含 userId 和 orderNo）
     */
    @Override
    public void onMessage(String message) {
        Long userId = JSON.parseObject(message).getLong("userId");
        String orderNo = JSON.parseObject(message).getString("orderNo");
        System.out.println(userId + "," + orderNo);
    }
}
