package com.example.orderservice.consumer;

import com.alibaba.fastjson.JSONObject;
import com.example.orderservice.client.GoodsClient;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.service.OrderService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单超时未支付取消消费者
 * <p>
 * 监听订单取消延时消息（下单后30min），检查订单状态，若仍未支付则取消订单并恢复库存
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-26
 */
@Component
@RocketMQMessageListener(topic = "order-cancel-topic",consumerGroup = "order-cancel-group-v2")
public class OrderCancelConsumer implements RocketMQListener<String> {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private GoodsClient goodsClient;

    /**
     * 处理订单取消消息
     * <p>
     * 1、解析订单号
     * 2、查询订单状态，若不存在或已支付/已取消则直接返回
     * 3、更新订单状态为已取消（status=2）
     * 4、查询订单明细，恢复各商品库存（调用商品服务）
     * </p>
     * @param message 消息内容（JSON），包含 orderNo
     */
    @Override
    public void onMessage(String message) {
        System.out.println("接收到的原始消息: " + message);
        JSONObject object = JSONObject.parseObject(message);
        String orderNo = object.getString("orderNo");
        Order order = orderMapper.orderInfoByOrderNo(orderNo);
        if (order == null||order.getStatus() != 0) {
            return;//订单不存在或状态已变更，无需处理
        }
        int rows = orderMapper.updateOrderStatusToCancel(orderNo);
        if (rows > 0) {
            List<OrderItem> items = orderMapper.findOrderItemsByOrderNo(orderNo);
            for (OrderItem item : items) {
                goodsClient.increaseStock(item.getProductId(),item.getQuantity());
                //订单状态未变更（超时未支付），恢复库存
            }
            System.out.println("订单超时取消，订单号："+orderNo);
        }

    }
}
