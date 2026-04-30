package com.example.goodservice.service;

import com.example.goodservice.entity.Goods;
import org.springframework.stereotype.Service;


public interface GoodService {
    Goods goodInfo(Long id);
    void reduceStock(Long id,Integer quantity);
}
