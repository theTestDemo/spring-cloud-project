package com.example.orderservice.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillGoods {
  private long id;
  private long goodsId;
  private BigDecimal seckillPrice;
  private long stock;
  private java.sql.Timestamp startTime;
  private java.sql.Timestamp endTime;
  private java.sql.Timestamp createTime;

}
