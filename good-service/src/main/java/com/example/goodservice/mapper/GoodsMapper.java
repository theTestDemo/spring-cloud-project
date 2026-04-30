package com.example.goodservice.mapper;

import com.example.goodservice.entity.Goods;
import com.example.goodservice.entity.StockLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GoodsMapper {
Goods goodInfo(@Param("id") Long id);
Integer reduceStock(@Param("id")Long id,
                    @Param("quantity")Integer quantity);
Integer insertStockLog(StockLog stockLog);
Integer increaseStock(@Param("id") Long id,
                      @Param("quantity")Long quantity);
}


