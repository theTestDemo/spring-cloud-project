package com.example.orderservice.mapper;

import com.example.orderservice.entity.SeckillGoods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SecKillGoodsMapper {
    List<SeckillGoods> selectValidSecKillGoods();
    Integer insertSecKillOrder(@Param("userId")Long userId,
                               @Param("goodsId") Long goodsId);
}
