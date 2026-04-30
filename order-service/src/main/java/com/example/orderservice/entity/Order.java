package com.example.orderservice.entity;

import com.example.orderservice.client.UserClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class Order {
    private Long id;
    private Long userId;
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer status;   // 0-待支付 1-已支付 2-已取消
    private Date createdTime;
}
