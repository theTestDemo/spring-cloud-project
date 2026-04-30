package com.example.orderservice.service;

import com.example.orderservice.dto.BuyGoodsDTO;
import com.example.orderservice.dto.ShoppingCartResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.ShoppingCart;
import com.example.orderservice.vo.PaymentVO;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {
    List<Order> getOrdersByUserId(Long userId);

    Order getOrderById(Long id);

    Order addOrder(Long userId, String orderNo, BigDecimal totalAmount,Integer status);

    void addShoppingCart(Long userId,Long productId, Long quantity);

    ShoppingCartResponse getShoppingCartByUserId(Long userId);

    BuyGoodsDTO buyGoods(Long userId);

    PaymentVO pay(Long userId,String orderN);
}
