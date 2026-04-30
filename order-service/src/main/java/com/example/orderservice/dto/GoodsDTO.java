package com.example.orderservice.dto;


import java.math.BigDecimal;

public class GoodsDTO {
    private long id;
    private long orderNo;
    private String name;
    private BigDecimal price;
    private long stock;
    private java.sql.Timestamp createTime;

    public long getId() {return id;}

    public void setId(long id) {this.id = id;}

    public long getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(long orderNo) {
        this.orderNo = orderNo;
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

}
