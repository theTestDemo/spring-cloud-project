package com.example.orderservice.mapper;

import com.example.orderservice.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
@Mapper
public interface PayMapper {
    Integer addPayment(@Param("payment") Payment payment);
}
