package com.example.goodservice.dto;

import lombok.Data;


@Data
public class AddReviewDTO {
    private String orderNo;      // 订单号
    private Long productId;      // 商品ID
    private Integer rating;      // 评分 1-5
    private String content;      // 评论内容
    private String pics;         // 图片URL
//    private Long userId;         //用户ID
//    private String reviewNo;   //评论编号
}