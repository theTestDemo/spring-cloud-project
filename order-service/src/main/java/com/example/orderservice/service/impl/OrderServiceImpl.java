package com.example.orderservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.common.domain.AjaxResult;
import com.example.common.exception.BusinessException;
import com.example.common.util.AjaxResultUtil;
import com.example.common.util.SnowflakeIdWorker;
import com.example.orderservice.client.GoodsClient;
import com.example.orderservice.client.UserClient;
import com.example.orderservice.dto.*;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.Payment;
import com.example.orderservice.entity.ShoppingCart;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.mapper.PayMapper;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.vo.PaymentVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * 订单服务实现类
 * <p>
 * 提供订单查询，创建等核心功能
 * 常见订单时会通过 Feign 调用用户服务验证用户是否存在
 * 并发送 RocketMQ 消息用于异步清理用户订单列表缓存
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-19
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private PayMapper payMapper;

    @Autowired
    private UserClient userClient;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SnowflakeIdWorker snowflakeIdWorker;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired private ApplicationEventPublisher applicationEventPublisher;


    /**
     * 根据用户 ID 查询订单列表
     *
     * @param userId 用户 Id
     * @return 订单列表，可能为空列表
     */
    @Override
    public List<Order> getOrdersByUserId(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    /**
     * 根据订单 ID 查询订单详情
     *
     * @param id 订单 ID
     * @return 订单实体，若不存在则返回 null
     */
    @Override
    public Order getOrderById(Long id) {
        return orderMapper.findById(id);
    }

    /**
     * 创建订单
     * <p>
     * 业务流程：
     * 1、调用用户服务验证用户是否存在
     * 2、若用户不存在，抛出业务异常
     * 3、若用户存在，插入订单记录
     * 4、发送订单创建消息到RocketMQ（用于异步清理用户订单列表缓存）
     * </p>
     *
     * @param userId      用户 ID
     * @param orderNo     订单号
     * @param totalAmount 订单总金额
     * @return 创建的订单对象（包含生成的自增 ID）
     * @throws BusinessException 当用户不存在时抛出业务异常
     */
    @Override
    public Order addOrder(Long userId, String orderNo, BigDecimal totalAmount, Integer status) {
        UserDTO exitUser = userClient.getUserById(userId);
        //后续已可从token获取userId，此处验证可优化
        if (exitUser == null) {
            throw new BusinessException("用户不存在");
        }
        orderMapper.addOrder(userId, orderNo, totalAmount, 0);
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNo(orderNo);
        order.setStatus(0);
        order.setTotalAmount(totalAmount);
        JSONObject msg = new JSONObject();
        msg.put("userId", userId);
        rocketMQTemplate.convertAndSend("order-add-topic", msg.toJSONString());
        return order;
    }

    /**
     * 添加商品到购物车
     * <p>
     * 若购物车内已有该商品，则仅修改库存
     * </p>
     *
     * @param userId
     * @param productId
     * @param quantity
     */
    @Override
    public void addShoppingCart(Long userId, Long productId, Long quantity) {
        int rows = orderMapper.addShoppingCartQuantity(userId, productId, quantity);
        //默认商品已存在购物车，直接修改库存，限制条件userId和productId，若未修改则说明商品不存在，添加商品信息至购物车
        if (rows == 0){
            int rows2 = orderMapper.addShoppingCart(userId, productId, quantity);
            if (rows2 == 0) {
                throw new BusinessException("添加购物车失败");
            }
        }

    }

    /**
     * 查询用户购物车详情
     * <p>
     * 1、从数据库查询购物车中的商品 ID 和数量
     * 2、遍历购物车，调用商品服务获取每个商品的名称、单价
     * 3、计算每个商品的小计和购物车总金额
     * 4、组装成 ShoppingCartResponse 返回
     * </p>
     *
     * @param userId 用户 ID
     * @return 购物车响应（包含总金额和商品列表），若购物车为空则总金额为 0， 列表为空
     */
    @Override
    public ShoppingCartResponse getShoppingCartByUserId(Long userId) {
        List<ShoppingCart> shoppingCarts = orderMapper.shoppingCartInfo(userId);
        if (shoppingCarts == null || shoppingCarts.isEmpty()) {
            return new ShoppingCartResponse(BigDecimal.ZERO, Collections.emptyList());
        }
        List<ShoppingCartDTO> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        //初始化购物车信息列表以及商品总价
        for (ShoppingCart item : shoppingCarts) {
            AjaxResult result = goodsClient.goodInfoById(item.getProductId());
            //调用商品信息feignClient获取购物车对应商品信息
            GoodsDTO goods = AjaxResultUtil.getData(result, GoodsDTO.class);
            if (goods == null) {
                continue;
            }
            BigDecimal subtotal = goods.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            //增加商品总价
            total = total.add(subtotal);
            //组装商品信息至购物车列表
            ShoppingCartDTO s = new ShoppingCartDTO();
            s.setProductId(item.getProductId());
            s.setProductName(goods.getName());
            s.setUnitPrice(goods.getPrice());
            s.setSubtotal(subtotal);
            s.setQuantity(item.getQuantity());
            items.add(s);
        }
        return new ShoppingCartResponse(total, items);
    }

    /**
     * 下单（从购物车结算）
     * <p>
     * 业务流程：
     * 1、使用分布式锁防止同一用户重复提交
     * 2、查询用户购物车、若为空则抛出异常
     * 3、生成订单号（雪花算法）
     * 4、便利购物车商品：
     *      a.调用商品服务获取商品信息（名称、单价）
     *      b.累加订单总金额
     *      c.构建订单明细对象
     *      d.调用商品服务扣减库存（若任一失败则整体回滚）
     * 5、插入订单主表（状态为待支付）
     * 6、批量插入订单明细
     * 7、清空购物车
     * 8、发送订单创建事件（由监听器在事务提交后异步处理通知）
     * 9、发送延时消息（正常逻辑为30min，此处设置为30s便于测试）
     * 10、返回订单信息（订单号、总金额、状态）
     * </p>
     * @param userId 用户ID
     * @return 订单信息（订单号、总金额、状态）
     * @throws BusinessException 购物车为空、商品信息获取失败、库存不足等
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BuyGoodsDTO buyGoods(Long userId) {
        String lockKey = "order:create:user:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试加锁，等待3秒，锁有效期30秒（可自动续期）
            boolean isLocked = lock.tryLock(3, 30, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException("操作过于频繁，请稍后再试");
            }
            List<ShoppingCart> shoppingCarts = orderMapper.shoppingCartInfo(userId);
            //获取用户购物车信息
            if (shoppingCarts == null || shoppingCarts.isEmpty()) {
                throw new BusinessException("购物车不能为空");
            }
            long id = snowflakeIdWorker.nextId();
            //通过雪花算法生成订单号
            String orderNo = String.valueOf(id);
            BigDecimal totalAmount = BigDecimal.ZERO;
            List<OrderItem> orderItems = new ArrayList<>();
            //创建订单明细列表，用于收集订单明细信息
            //计算总价
            for (ShoppingCart item : shoppingCarts) {
                AjaxResult ajaxResult = goodsClient.goodInfoById(item.getProductId());
                if (!ajaxResult.isSuccess()) {
                    throw new BusinessException("商品信息获取失败");
                }
                GoodsDTO goods = AjaxResultUtil.getData(ajaxResult, GoodsDTO.class);

                //便利购物车，通过购物车商品信息中的商品id跨模块获取商品信息
                totalAmount = totalAmount.add(goods.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                //商品单价*购物车数量，叠加计算总计
                //orderMapper.addOrderItem(orderNo, goods.getId(), item.getQuantity(), goods.getPrice());
                //添加订单明细
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderNo(orderNo);
                orderItem.setProductId(item.getProductId());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setUnitPrice(goods.getPrice());
                orderItems.add(orderItem);
                //收集订单明细信息
                AjaxResult ajaxResultReduce = goodsClient.reduceStock(goods.getId(), (int) item.getQuantity());
                if (!ajaxResultReduce.isSuccess()) {
                    throw new BusinessException("库存扣减失败");
                }
                //扣库存
            }
            orderMapper.addOrder(userId, orderNo, totalAmount, 0);
            //创建订单
            orderMapper.batchAddOrderItem(orderItems);
            BuyGoodsDTO goods = new BuyGoodsDTO();
            goods.setOrderNo(orderNo);
            goods.setStatus(0);
            goods.setTotalAmount(totalAmount);
            //清空购物车
            orderMapper.deleteShoppingCart(userId);
            log.info(">>> 即将发布 OrderCreatedEvent");
            //发布订单创建事件（事务提交后由 OrderEventListener 异步处理）
            applicationEventPublisher.publishEvent(
                    new OrderCreatedEvent(this,userId.toString(),orderNo)
            );
            log.info(">>> 已发布 OrderCreatedEvent");
            Map<String, Object> map = new HashMap<>();
            map.put("orderNo", orderNo);
            Message<String> message = MessageBuilder.withPayload(JSON.toJSONString(map)).build();
            rocketMQTemplate.syncSend("order-cancel-topic", message, 1000, 16);
            log.info("下单成功，订单号：{}，准备发布事件", orderNo);
            return goods;
            // 原有下单逻辑（查询购物车、扣库存、生成订单、清空购物车）
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

    /**
     * 支付订单
     * <p>
     * 业务流程：
     * 1、根据用户 ID 和订单号查询订单，若不存在或不属于当前用户则抛出异常
     * 2、更新订单状态为已支付（使用乐观锁，条件为 status=0，防止重复支付）
     * 3、生成支付流水（支付时间、交易流水号、支付金额）
     * 4、插入支付记录表
     * 5、发送支付成功消息到 RocketMQ （用于后续积分、物流等异步处理）
     * 6、返回支付信息（订单号、金额、交易流水号、支付时间）
     * </p>
     *
     * @param userId   用户ID（从请求头获取，用于校验权限）
     * @param orderNo  订单号
     * @return 支付结果（订单号、金额、交易流水号、支付时间）
     * @throws BusinessException 订单不存在、订单已支付、流水插入失败等
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO pay(Long userId, String orderNo) {
        Order order = orderMapper.orderInfoByOrderNoUserId(userId, orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        //确认用户信息，查询订单信息
        int rows = orderMapper.updateOrderStatusToPaid(userId, orderNo);
        if (rows == 0) {
            throw new BusinessException("订单已支付");
        }
        //修改订单状态为已支付

        //添加实际支付业务，此处作为功能演示，默认支付成功
        Date payTime = new Date();
        String transactionId = String.valueOf(snowflakeIdWorker.nextId());
        Payment payment = new Payment();
        payment.setOrderNo(orderNo);
        payment.setAmount(order.getTotalAmount());
        payment.setPayTime(payTime);
        payment.setTransactionId(transactionId);
        payment.setStatus(1);
        int insertPaymentRows = payMapper.addPayment(payment);
        if (insertPaymentRows == 0) {
            throw new BusinessException("流水明细插入失败");
        }
        //组装流水明细实体类，插入流水表
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("orderNo", orderNo);
        rocketMQTemplate.convertAndSend("order-paid-topic", map);
        //向RocketMQ发送消息，可添加扣除积分，物流等异步处理
        PaymentVO vo = new PaymentVO();
        vo.setTransactionId(transactionId);
        vo.setPayTime(payTime);
        vo.setOrderNo(orderNo);
        vo.setAmount(order.getTotalAmount());
        //组装vo，返回前端
        return vo;
    }


}