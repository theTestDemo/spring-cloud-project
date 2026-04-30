package com.example.orderservice.consumer;

import com.alibaba.fastjson.JSONObject;
import com.example.common.domain.AjaxResult;
import com.example.common.exception.BusinessException;
import com.example.orderservice.client.GoodsClient;
import com.example.orderservice.entity.Order;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.service.OrderService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 秒杀订单消费者
 * <p>
 * 监听’secKill-topic‘主题，消费秒杀成功消息，异步创建正式订单
 * 幂等性设计：根据订单号查询订单是否已存在，若存在则直接返回，避免重复创建
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-26
 */
@Component
@RocketMQMessageListener(topic = "secKill-topic", consumerGroup = "secKill-group")
public class SecKillConsumer implements RocketMQListener<String> {
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private GoodsClient goodsClient;

    /**
     * 处理秒杀成功消息
     * <p>
     * 1、解析消息获取用户ID、商品ID、秒杀价格、订单号
     * 2、幂等性检查：防止重复消费导致重复创建订单
     * 3、插入订单主表（status=0 待支付）
     * 4、插入订单明细表（数量固定为1，价格为秒杀价）
     * 5、不在此处扣减秒杀库存（库存已在秒杀服务中通过 Redis 扣减）
     * </p>
     * @param message 消息体（JSON格式）
     */
    @Override
    public void onMessage(String message) {
        Long userId = JSONObject.parseObject(message).getLong("userId");
        Long goodsId = JSONObject.parseObject(message).getLong("goodsId");
        BigDecimal secKillPrice = JSONObject.parseObject(message).getBigDecimal("secKillPrice");
        String orderNo = JSONObject.parseObject(message).getString("orderNo");
        Order order = orderMapper.orderInfoByOrderNo(orderNo);
        //幂等性检查：如果订单已存在，直接返回
        if (order != null) {
            System.out.println("订单已创建，幂等消费，订单号：" + orderNo);
            return;
        }
        //创建订单主表
        int rows = orderMapper.addOrder(userId, orderNo, secKillPrice, 0);
        if (rows == 0) {
            throw new BusinessException("订单创建失败");
        }
        //创建订单明细
        int rows1 = orderMapper.addOrderItem(orderNo, goodsId, 1, secKillPrice);
        if (rows1 == 0) {
            throw new BusinessException("订单明细创建失败");
        }
    }
}
