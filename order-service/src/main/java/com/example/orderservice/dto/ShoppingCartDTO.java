package com.example.orderservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ShoppingCartDTO {
    private Long productId;          // 来源：shopping_cart.product_id（原数据库字段）
    private String productName;      // 来源：从商品服务 Feign 调用获取（goods.name）
    private BigDecimal unitPrice;    // 来源：从商品服务 Feign 调用获取（goods.price）
    private long quantity;        // 来源：shopping_cart.quantity（原数据库字段）
    private BigDecimal subtotal;     // 计算值：unitPrice * quantity（自己计算）
}