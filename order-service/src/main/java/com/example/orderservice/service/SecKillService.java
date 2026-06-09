package com.example.orderservice.service;

/**
 * 秒杀服务接口
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
public interface SecKillService {
    /**
     * 商品秒杀
     */
    String SecKill(Long userId,Long goodId);
}
