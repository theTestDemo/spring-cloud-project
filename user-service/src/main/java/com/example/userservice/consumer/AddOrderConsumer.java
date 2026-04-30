package com.example.userservice.consumer;

import com.alibaba.fastjson.JSON;
import com.example.common.util.RedisUtil;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 订单创建消息消费者
 * <p>
 * 监听 RocketMQ 的 {@code order-add-topic} 主题，消费订单创建成功消息。
 * 当收到消息时，解析其中的用户 ID，并删除该用户的订单列表缓存（Redis），
 * 确保用户下次查询订单列表时能获取最新数据（最终一致性）。
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
@Component
@RocketMQMessageListener(topic = "order-add-topic", consumerGroup = "order-cache-cleaner")
public class AddOrderConsumer implements RocketMQListener<String> {
    @Autowired
    private RedisUtil redisUtil;

    /**
     * 消费订单创建消息
     * <p>
     * 消息体为 JSON 字符串，例如：{"userId": 1}
     * 解析后删除 Redis 中对应 key 的订单列表缓存
     * </p>
     *
     * @param message 消息内容（JSON 字符串格式）
     */
    @Override
    public void onMessage(String message) {
        Long userId = JSON.parseObject(message).getLong("userId");
        //mq发送的消息是JSON的String格式，需要使用parseObject方法解析
        String key = "user:orders:list:" + userId;
        redisUtil.delete(key);
        System.out.println("已删除用户 " + userId + " 的订单列表缓存");
    }
}
