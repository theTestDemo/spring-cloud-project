package com.example.orderservice.service;

import com.example.orderservice.dto.OrderReviewDTO;
import com.example.orderservice.dto.BuyGoodsDTO;
import com.example.orderservice.dto.ShoppingCartResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.vo.PaymentVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单服务接口
 *
 * @author 胡孟阳
 * @since 2026-4-19
 */
public interface OrderService {

    /**
     * 根据用户 ID 查询订单列表
     */
    List<Order> getOrdersByUserId(Long userId);

    /**
     * 根据订单 ID 查询订单详情
     */
    Order getOrderById(Long id);

    /**
     * 创建订单
     * <p>业务流程：验证用户 → 插入订单 → 发送消息通知</p>
     */
    Order addOrder(Long userId, String orderNo, BigDecimal totalAmount, Integer status);

    /**
     * 添加商品到购物车
     * <p>若已存在则修改数量，若不存在则新增</p>
     */
    void addShoppingCart(Long userId, Long productId, Long quantity);

    /**
     * 查询用户购物车详情
     * <p>调用商品服务获取商品信息，计算总金额</p>
     */
    ShoppingCartResponse getShoppingCartByUserId(Long userId);

    /**
     * 下单（从购物车结算）
     * <p>业务流程：分布式锁 → 查询购物车 → 扣库存 → 生成订单 → 清空购物车 → 发送延时消息</p>
     */
    BuyGoodsDTO buyGoods(Long userId);

    /**
     * 支付订单
     * <p>业务流程：验证订单 → 更新状态（乐观锁）→ 生成支付流水 → 发送支付成功消息</p>
     */
    PaymentVO pay(Long userId, String orderNo);

    /**
     * 根据订单号查询订单详情
     */
    Order orderInfoByOrderNo(String orderNo);

    /**
     * 查询订单信息（用于商品评论验证）
     */
    OrderReviewDTO orderReview(String orderNo);
}