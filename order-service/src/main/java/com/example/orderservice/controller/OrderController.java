package com.example.orderservice.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.common.domain.AjaxResult;
import com.example.common.exception.BusinessException;
import com.example.orderservice.dto.BuyGoodsDTO;
import com.example.orderservice.dto.GoodsDTO;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Payment;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.vo.PaymentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单服务 REST 控制器
 * <p>
 * 提供订单查询、创建等 API 接口
 * 创建订单时会通过 Feign 调用用户服务是否存在，并发送消息清理用户订单列表缓存
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 根据用户 ID 查询订单列表
     *
     * @param userId 用户 ID
     * @return 订单列表
     */
    @GetMapping("/user/{userId}")
    public List<Order> getOrdersByUserId(@PathVariable Long userId) {
        return orderService.getOrdersByUserId(userId);
    }

    /**
     * 根据订单 ID 查询订单详情
     *
     * @param id 订单 ID
     * @return 订单实体
     */
    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    /**
     * 创建订单
     * <p>
     * 业务流程：
     * 1、调用用户服务验证用户是否存在（用过 Feign）
     * 2、插入订单记录
     * 3、发送消息通知用户服务清理缓存
     * </p>
     *
     * @param userId      用户 ID
     * @param orderNo     订单号（需唯一）
     * @param totalAmount 订单总金额
     * @return 创建成功的订单对象
     */
    @PostMapping("/addOrder")
    public ResponseEntity<AjaxResult> addOrder(@RequestParam Long userId,
                                               @RequestParam String orderNo,
                                               @RequestParam BigDecimal totalAmount,
                                               @RequestParam Integer status) {
        Order order = orderService.addOrder(userId, orderNo, totalAmount, status);
        return ResponseEntity.ok(AjaxResult.success(order));
    }

    /**
     * 添加商品到购物车
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @param quantity  数量
     * @return 操作成功响应
     */
    @PostMapping("/addShoppingCart")
    public ResponseEntity<AjaxResult> addShoppingCart(@RequestParam Long userId,
                                                      @RequestParam Long productId,
                                                      @RequestParam Long quantity) {
        orderService.addShoppingCart(userId, productId, quantity);
        return ResponseEntity.ok(AjaxResult.success());
    }

    /**
     * 查询用户购物车消息
     * <p>
     * 返回购物车中的商品列表，包括商品名称、单价、数量、小计及总金额
     * 数据通过 Feign 调用商品服务获取商品详情
     * </p>
     *
     * @param userId 用户 ID
     * @return 购物车详情
     */
    @GetMapping("shoppingCartInfo")
    public ResponseEntity<AjaxResult> shoppingCartInfo(@RequestParam Long userId) {
        return ResponseEntity.ok(AjaxResult.success(orderService.getShoppingCartByUserId(userId)));
    }

    /**
     * 下单（从购物车结算）
     * <p>
     * 业务流程：
     * 1、查询用防护购物车商品
     * 2、调用商品服务扣减库存
     * 3、生成订单号（雪花算法）
     * 4、批量插入订单明细
     * 5、插入订单主表
     * 6、清空购物车
     * 7、返回订单信息
     * </p>
     *
     * @param userId 用户 ID
     * @return 订单号、总金额、状态
     */
    @GetMapping("buyGoods")
    @SentinelResource(value = "buyGoods", blockHandler = "buyGoodsBlockHandler")
    public ResponseEntity<AjaxResult> buyGoods(@RequestParam Long userId) {
        BuyGoodsDTO goods = orderService.buyGoods(userId);
        return ResponseEntity.ok(AjaxResult.success(goods));
    }

    /**
     * 下单接口限流降级处理
     * <p>
     * 当 {@code buyGoods} 接口触发 Sentinel 限流规则（例如 QPS 超过阈值）时，会调用此方法返回友好提示
     * 限流规则在 {@link com.example.orderservice.config.SentinelConfig} 中配置，资源名为{@code buyGoods}
     * </p>
     *
     * @param userId 用户 ID （于原方法参数一致）
     * @param ex     Sentinel 限流异常，包含触发降级的原因
     * @return 统一的限流响应，HTTP 状态码为 409 （Too Many Request）
     */
    public ResponseEntity<AjaxResult> buyGoodsBlockHandler(Long userId, BlockException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(AjaxResult.error("系统繁忙，请稍后重试"));
    }

    /**
     * 支付订单
     * <p>
     * 根据订单号支付订单，更新订单状态为已支付，并记录支付流水
     * 使用乐观锁防止重复支付（通过更新订单状态时检查原状态）
     * 支付成功后记录支付流水，并返回支付信息
     * </p>
     *
     * @param userId   用户Id(从请求头 X-User-Id 获取，用于权限校验)
     * @param orderNo  订单号
     * @return 支付结果，包含订单号、支付金额、交易流水号，交易时间。
     */
    @GetMapping("pay")
    public ResponseEntity<AjaxResult> pay(@RequestHeader("X-User-Id") Long userId,
                                          @RequestParam String orderNo) {
        PaymentVO paymentVO = orderService.pay(userId, orderNo);
        return ResponseEntity.ok(AjaxResult.success(paymentVO));
    }
}