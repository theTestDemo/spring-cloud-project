package com.example.goodservice.mapper;

import com.example.goodservice.dto.AddReviewDTO;
import com.example.goodservice.entity.Goods;
import com.example.goodservice.entity.ProductReview;
import com.example.goodservice.entity.StockLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GoodsMapper {
Goods goodInfo(@Param("id") Long id);
Integer reduceStock(@Param("id")Long id,
                    @Param("quantity")Integer quantity);
Integer insertStockLog(StockLog stockLog);
Integer increaseStock(@Param("id") Long id,
                      @Param("quantity")Long quantity);
int reviewPlagiarismCheck(@Param("orderNo")String orderNo,
                           @Param("productId") Long productId);
Integer insertReview(ProductReview review);
Integer updateReview(@Param("rating") Integer rating,
                     @Param("productId") Long productId);
List<ProductReview> review(@Param("productId") Long productId,@Param("sort") String sort);
Integer countReviews(@Param("productId") Long productId);
ProductReview reviewInfoByNo(@Param("reviewNo") String reviewNo);
Integer delReview(@Param("reviewNo") String reviewNo);
Integer rollbackReview(@Param("productId") Long productId,
                       @Param("deletedRating") Long deletedRating);
}


