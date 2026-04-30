package com.example.orderservice.fallback;

import com.example.common.domain.AjaxResult;
import com.example.orderservice.client.GoodsClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * 商品服务 Feign 客户端降级工厂
 * <p>
 * 当商品服务（good-service）不可用或调用失败时，Sentinel 会触发此降级工厂
 * 为{@link GoodsClient} 中的所有方法提供备选响应，避免因依赖故障导致订单服务异常
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-22
 */
@Component
public class GoodsInfoFallbackFactory implements FallbackFactory<GoodsClient> {
    /**
     * 床渐渐降级后的 GoodsClient 代理
     *
     * @param cause 远程调用失败的原因（异常信息）
     * @return 降级后的 GoodsClient 实例
     */
    @Override
    public GoodsClient create(Throwable cause) {
        return new GoodsClient() {
            /**
             * 商品信息查询降级
             * @param id 商品ID
             * @return 错误响应（业务码500）
             */
            @Override
            public AjaxResult goodInfoById(Long id) {
                return AjaxResult.error("获取商品信息失败");
            }

            /**
             * 扣减库存降级
             * @param id        商品ID
             * @param quantity  扣减数量
             * @return 错误响应（业务码500）
             */
            @Override
            public AjaxResult reduceStock(Long id, Integer quantity) {
                System.out.println("======================");
                return AjaxResult.error("商品信息不可用，库存扣减失败");
            }

            /**
             * 恢复库存降级
             * @param id        商品ID
             * @param quantity  恢复数量
             * @return 错误响应（业务码500）
             */
            @Override
            public AjaxResult increaseStock(Long id, Long quantity) {
                return AjaxResult.error("库存恢复失败");
            }
        };
    }
}
