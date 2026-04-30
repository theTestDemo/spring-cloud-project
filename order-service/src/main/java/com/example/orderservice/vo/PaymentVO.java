package com.example.orderservice.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
@Data
public class PaymentVO {
    private String OrderNo;
    private BigDecimal amount;
    private String transactionId;
    private Date payTime;
}
