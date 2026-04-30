package com.example.userservice.consumer;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 用户服务消息消费者
 * <p>
 * 监听 RocketMQ 的 {@code user-topic} 主题，消费用户相关消息（如用户数据加载完成通知）
 * 该消费者属于 {@code user-consumer-group} 消费者组，采用集群消费模式
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
@Component
@RocketMQMessageListener(topic = "user-topic", consumerGroup = "user-consumer-group")
public class UserMessageConsumer implements RocketMQListener<String> {
    /**
     * 消费消息的处理方法
     *
     * <p>
     * 当收到 {@code user-topic} 主题的消息时，该方法会被自动调用
     * 当前实现仅打印消息内容，实际业务可扩展为更新缓存、记录日志等
     * </p>
     *
     * @param message 消息内容（字符串格式）
     */
    @Override
    public void onMessage(String message) {
        System.out.println("消费者收到消息: " + message);
    }
}
