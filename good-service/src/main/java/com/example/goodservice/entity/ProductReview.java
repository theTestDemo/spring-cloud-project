package com.example.goodservice.entity;

import lombok.Data;

import java.util.Date;

@Data
public class ProductReview {
  private long id;
  private String reviewNo;
  private long userId;
  private long productId;
  private String orderNo;
  private long rating;
  private String content;
  private String pics;
  private long status;
  private Date createTime;
  private Date updateTime;
}
