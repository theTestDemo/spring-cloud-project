package com.example.orderservice.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
@Data
public class BuyGoodsDTO {
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer status;
    private Date createTime;
}
