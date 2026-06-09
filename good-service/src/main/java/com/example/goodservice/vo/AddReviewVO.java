package com.example.goodservice.vo;

import lombok.Data;

import java.util.Date;
@Data
public class AddReviewVO {
    private String reviewNo;
    private Long productId;
    private Integer rating;
    private String content;
    private String pics;
    private Date createTime;
}
