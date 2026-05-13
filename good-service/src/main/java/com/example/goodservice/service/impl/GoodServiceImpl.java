package com.example.goodservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.common.exception.BusinessException;
import com.example.common.util.RedisUtil;
import com.example.goodservice.entity.Goods;
import com.example.goodservice.entity.StockLog;
import com.example.goodservice.mapper.GoodsMapper;
import com.example.goodservice.service.GoodService;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
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
    
    @Autowired
    private RedissonClient redissonClient;

    private static final String GOODS_CACHE_PREFIX = "goods:";


    /**
     * 根据商品 ID 查询商品信息（带缓存）
     * <p>
     * 业务流程：
     * 1、先查 Redis 缓存，命中则直接返回（如果是空对象标记则抛异常）
     * 2、缓存未命中，获取分布式锁（Redisson），防止缓存击穿
     * 3、双重检查：锁内再次查询缓存，避免重复加载
     * 4、查数据库，若不存在则缓存空对象（过期 60 秒），避免缓存穿透
     * 5、若存在则缓存商品信息（过期 300 秒），并释放锁
     * </p>
     * @param id 商品ID
     * @return 商品实体
     * @throws BusinessException
     */
    @Override
    public Goods goodInfo(Long id) {
//        String key = GOODS_CACHE_PREFIX + id;
//        String json = redisUtil.get(key);
//        if (json != null) {
//            return JSON.parseObject(json, Goods.class);
//        }
//
//        Goods goods = goodsMapper.goodInfo(id);
//        if (goods == null) {
//            throw new BusinessException("商品信息不存在");
//        }
//        redisUtil.set(key, JSON.toJSONString(goods), 300, TimeUnit.SECONDS);
//        return goods;
        //原逻辑
        String key = GOODS_CACHE_PREFIX + id;
        //查询缓存
        String json = redisUtil.get(key);
        if (json != null) {
            if ("null".equals(json)) {
                throw new BusinessException("商品信息不存在");
            }
            return JSON.parseObject(json, Goods.class);
        }
        String lockKey = "goods:lock:" + id;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            //尝试获取锁，防止击穿
            if (!lock.tryLock(3,10,TimeUnit.SECONDS)){
                throw new BusinessException("系统繁忙，请稍后再试");
            }
            //多重检查：锁内再次查询缓存
            json = redisUtil.get(key);
            if (json != null) {
                if ("null".equals(json)) {
                    throw new BusinessException("商品信息不存在");
                }
                return JSON.parseObject(json, Goods.class);
            }
            //查数据库
            Goods goods = goodsMapper.goodInfo(id);
            if (goods == null){
                //缓存空对象，防止穿透
                redisUtil.set(key,"null",60,TimeUnit.SECONDS);
                throw new BusinessException("商品信息不存在");
            }
            //缓存商品信息
            redisUtil.set(key,JSON.toJSONString(goods),300,TimeUnit.SECONDS);
            return goods;
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            throw new BusinessException("系统繁忙，请稍后再试");
        }finally {
            if (lock.isHeldByCurrentThread()){
                //释放锁
                lock.unlock();
            }
        }
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
