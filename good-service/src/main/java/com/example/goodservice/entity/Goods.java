package com.example.goodservice.entity;


import java.math.BigDecimal;

public class Goods {

  private long id;
  private String name;
  private BigDecimal price;
  private long stock;
  private java.sql.Timestamp createTime;
  private Integer reviewCount;
  private BigDecimal avgRating;


  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }


  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }


  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }


  public long getStock() {
    return stock;
  }

  public void setStock(long stock) {
    this.stock = stock;
  }


  public java.sql.Timestamp getCreateTime() {
    return createTime;
  }

  public void setCreateTime(java.sql.Timestamp createTime) {
    this.createTime = createTime;
  }

  public Integer getReviewCount() {
    return reviewCount;
  }

  public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

  public BigDecimal getAvgRating() { return avgRating;}

  public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating;}

}
