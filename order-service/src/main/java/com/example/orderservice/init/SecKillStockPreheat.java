package com.example.orderservice.init;

import com.example.orderservice.entity.SeckillGoods;
import com.example.orderservice.mapper.SecKillGoodsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 秒杀库存预热组件
 * <p>
 * 在应用启动时，从数据库加载所有有效秒杀商品（stock>0 且在活动时间内）的库存和价格，
 * 存入 Redis，用于秒杀时的高性能原子扣减
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-26
 */
@Component
public class SecKillStockPreheat implements CommandLineRunner {
    @Autowired
    SecKillGoodsMapper secKillGoodsMapper;
    @Autowired
    StringRedisTemplate redisTemplate;

    /**
     * 应启动后自动执行
     * @param args 启动参数
     */
    @Override
    public void run(String... args) throws Exception {
        List<SeckillGoods> list = secKillGoodsMapper.selectValidSecKillGoods();
        for (SeckillGoods sg : list) {
            String key = "secKill:stock:"+sg.getGoodsId();
            String priceKey = "secKill:price:"+sg.getGoodsId();
            redisTemplate.opsForValue().set(key,String.valueOf(sg.getStock()));
            //向 Redis 添加秒杀商品库存
            redisTemplate.opsForValue().set(priceKey,String.valueOf(sg.getSeckillPrice()));
            //向 Redis 添加秒杀商品价格
        }
        System.out.println("秒杀商品预热已完成,加载商品数："+list.size());

    }
}
