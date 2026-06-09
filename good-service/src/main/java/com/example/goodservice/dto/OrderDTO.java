package com.example.goodservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class OrderDTO{
    private Long userId;
    private String orderNo;
    private Integer status;   // 0-待支付 1-已支付 2-已取消
    private List<Long> productId;
}
