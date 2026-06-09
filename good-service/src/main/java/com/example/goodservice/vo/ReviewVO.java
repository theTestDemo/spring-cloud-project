package com.example.goodservice.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ReviewVO {

    /** 评论编号 */
    private String reviewNo;

    /** 评论者用户ID */
    private Long userId;

    /** 评论者昵称（来自 user-service） */
    private String username;

    /** 评论者头像 URL（来自 user-service） */
    private String avatar;

    /** 评分 1-5 */
    private long rating;

    /** 评论文字内容 */
    private String content;

    /** 图片 URL 数组（数据库存逗号分隔字符串，返回时拆成数组） */
    private List<String> pics;

    /** 评论时间 */
    private Date createTime;
}
