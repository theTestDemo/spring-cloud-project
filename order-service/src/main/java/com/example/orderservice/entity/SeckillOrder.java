package com.example.orderservice.entity;

import lombok.Data;
@Data
public class SeckillOrder {
  private long id;
  private long userId;
  private long goodsId;
  private String orderNo;
  private java.sql.Timestamp createTime;
}
