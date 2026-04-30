package com.example.orderservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.common.domain.AjaxResult;
import com.example.common.exception.BusinessException;
import com.example.common.util.AjaxResultUtil;
import com.example.common.util.SnowflakeIdWorker;
import com.example.orderservice.dto.BuyGoodsDTO;
import com.example.orderservice.dto.GoodsDTO;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.ShoppingCart;
import com.example.orderservice.mapper.SecKillGoodsMapper;
import com.example.orderservice.service.SecKillService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀服务实现类
 * <p>
 * 提供秒杀核心功能，包括：
 * - 分布式锁防止同一用户并发秒杀
 * - 数据库防重（唯一索引）防止重复秒杀
 * - Redis 原子扣库存（高并发核心）
 * - 发送秒杀成功消息，异步创建正式订单
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-26
 */
@Service
public class SecKillServiceImpl implements SecKillService {

    @Autowired
    private SecKillGoodsMapper secKillGoodsMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private SnowflakeIdWorker snowflakeIdWorker;

    private final static String SEC_KILL_STOCK_CACHE_PREFIX = "secKill:stock:";

    /**
     * 秒杀核心业务逻辑
     * <p>
     * 流程：
     * 1、分布式锁（Redisson）防止同一用户重复请求
     * 2、防重表插入（利用唯一索引拦截重复秒杀）
     * 3、Redis 原子扣库存（decrement，负数则回滚）
     * 4、生成唯一订单号（雪花算法）
     * 5、发送秒杀成功消息到 RecktMQ（异步创建正式订单）
     * 6、释放锁并返回订单号
     * </p>
     * <p>
     * 注意：秒杀库存仅存储在 Redis，未双写数据以追求极致性能；
     * 正式订单由消息消费者异步创建，保证最终一致性
     * </p>
     * @param userId 用户ID（从网关解析的请求头 X-User-Id 获取）
     * @param goodId 秒杀商品ID
     * @return 订单号（字符串）
     * @throws BusinessException 操作频繁、重复秒杀、库存不足、商品不存在等
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String SecKill(Long userId, Long goodId) {
        String lockKey = "secKill:user:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试加锁，等待3秒，锁有效期30秒（可自动续期）
            boolean isLocked = lock.tryLock(3, 30, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException("操作过于频繁，请稍后再试");
            }


            try {
                secKillGoodsMapper.insertSecKillOrder(userId, goodId);
                //插入查重表，用户、商品id唯一，插入失败则直接抛出异常
            }catch (Exception e){
                throw new BusinessException("不可重复购买");
            }
            String key = SEC_KILL_STOCK_CACHE_PREFIX + goodId;

            Long stock = redisTemplate.opsForValue().decrement(key);//扣减redis库存
            if (stock < 0) {//库存扣减后为负
                redisTemplate.opsForValue().increment(key);
                throw new BusinessException("库存不足");//回滚库存，抛出异常
            }
            Map<String, Object> map = new HashMap<>();
            String secKillPrice = redisTemplate.opsForValue().get("secKill:price:"+goodId);
            if (secKillPrice == null){
                throw  new BusinessException("秒杀商品不存在");
            }
            BigDecimal price = new BigDecimal(secKillPrice);
            //雪花算法生成订单号
            Long orderNo = snowflakeIdWorker.nextId();
            map.put("userId", userId);
            map.put("goodsId", goodId);
            map.put("secKillPrice", price);
            map.put("orderNo", orderNo);
            rocketMQTemplate.convertAndSend("secKill-topic",map);
            return orderNo.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("系统繁忙，请稍后再试");
        } finally {
            // 释放锁（只有当前线程持有锁时才释放）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
