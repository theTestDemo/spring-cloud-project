package com.example.goodservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.common.exception.BusinessException;
import com.example.common.util.RedisUtil;
import com.example.goodservice.entity.Goods;
import com.example.goodservice.entity.StockLog;
import com.example.goodservice.mapper.GoodsMapper;
import com.example.goodservice.service.GoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现类
 * <p>
 * 提供商品信息查询、库存扣减及库存变更日志记录功能
 * 提供 Redis 缓存商品信息，提升查询性能；扣减库存时记录日志，便于审计
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
@Service
public class GoodServiceImpl implements GoodService {
    @Autowired
    public GoodsMapper goodsMapper;

    @Autowired
    public RedisUtil redisUtil;

    private static final String GOODS_CACHE_PREFIX = "goods:";

    /**
     * 根据商品 ID 查询商品信息（带缓存）
     * <p>
     * 先查 Redis 缓存，命中则直接返回；未命中则查询数据库，并将结果写入缓存（过期时间 300s）
     * </p>
     *
     * @param id 商品ID
     * @return 商品实体类
     * @throws BusinessException 商品不存在时抛出
     */
    @Override
    public Goods goodInfo(Long id) {
        String key = GOODS_CACHE_PREFIX + id;
        String json = redisUtil.get(key);
        if (json != null) {
            return JSON.parseObject(json, Goods.class);
        }

        Goods goods = goodsMapper.goodInfo(id);
        if (goods == null) {
            throw new BusinessException("商品信息不存在");
        }
        redisUtil.set(key, JSON.toJSONString(goods), 300, TimeUnit.SECONDS);
        return goods;
    }

    /**
     * 扣减商品库存（乐观锁+日志记录）
     * <p>
     * 业务流程：
     * 1、执行原子扣减 SQL(UPDATE ... SET stock = stock - quantity WHERE id = #{id} AND stock >= #{quantity})
     * 2、如果影响行数为0，说明宁库存不足或商品不存在，抛出业务异常
     * 3、扣减成功后，记录库存变更日志（包括变更前后库存、变更数量）
     * 4、伤处 Redis 缓存，确保下次查询获取最新数据
     * </p>
     *
     * @param id        商品ID
     * @param quantity  扣减数量（正数）
     * @throws BusinessException 库存不足或商品不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reduceStock(Long id, Integer quantity) {
        int rows = goodsMapper.reduceStock(id, quantity);
        if (rows == 0) {
            // 扣减失败，可能是库存不足或商品不存在
            // 可以再次查询以区分，但为了简单，统一提示库存不足
            throw new BusinessException("库存不足或商品不存在");
        } else {
            Goods goods = goodsMapper.goodInfo(id);
            StockLog stockLog = new StockLog();
            stockLog.setProductId(id);
            stockLog.setChangeAmount(-quantity);
            stockLog.setAfterStock(goods.getStock());//扣减后的库存
            stockLog.setBeforeStock(goods.getStock() + quantity);//扣减前的库存
            goodsMapper.insertStockLog(stockLog);
        }
        // 删除缓存，保证数据一致性
        redisUtil.delete(GOODS_CACHE_PREFIX + id);

    }



}
