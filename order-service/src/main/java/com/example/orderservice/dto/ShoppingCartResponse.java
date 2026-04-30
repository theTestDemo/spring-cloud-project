package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class ShoppingCartResponse {
    private BigDecimal totalAmount;       // 计算值：所有 CartItemDTO.subtotal 之和（自己计算）
    private List<ShoppingCartDTO> items;      // 组装后的购物车商品列表（包含原数据库字段 + 获取的商品信息 + 计算值）
}