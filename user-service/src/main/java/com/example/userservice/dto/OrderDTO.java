package com.example.userservice.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class OrderDTO {
    private Long id;
    private Long userId;
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer status;
    private Date createdTime;
}