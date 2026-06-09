package com.example.goodservice.service;

import com.example.goodservice.dto.AddReviewDTO;
import com.example.goodservice.entity.Goods;
import com.example.goodservice.vo.AddReviewVO;
import com.example.goodservice.vo.ReviewVO;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

/**
 * 商品服务接口
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
public interface GoodService {
    /**
     * 根据商品ID查询商品信息（带缓存）
     */
    Goods goodInfo(Long id);

    /**
     * 扣减商品库存（乐观锁+日志记录）
     */
    void reduceStock(Long id,Integer quantity);

    /**
     * 添加商品评论
     */
    AddReviewVO addReview(Long userId, String username, AddReviewDTO addReviewDTO);

    /**
     * 分页查询商品评论列表
     */
    PageInfo<ReviewVO> review(Long productId, Integer page, Integer pageSize, String sort);

    /**
     * 删除商品评论
     */
    void delReview(Long userId,String reviewNo);
}
