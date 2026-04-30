package com.example.goodservice.entity;


public class StockLog {

  private long id;
  private long productId;
  private long changeAmount;
  private long beforeStock;
  private long afterStock;
  private java.sql.Timestamp createTime;


  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }


  public long getProductId() {
    return productId;
  }

  public void setProductId(long productId) {
    this.productId = productId;
  }


  public long getChangeAmount() {
    return changeAmount;
  }

  public void setChangeAmount(long changeAmount) {
    this.changeAmount = changeAmount;
  }


  public long getBeforeStock() {
    return beforeStock;
  }

  public void setBeforeStock(long beforeStock) {
    this.beforeStock = beforeStock;
  }


  public long getAfterStock() {
    return afterStock;
  }

  public void setAfterStock(long afterStock) {
    this.afterStock = afterStock;
  }


  public java.sql.Timestamp getCreateTime() {
    return createTime;
  }

  public void setCreateTime(java.sql.Timestamp createTime) {
    this.createTime = createTime;
  }

}
