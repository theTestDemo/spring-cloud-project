package com.example.goodservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.common.domain.AjaxResult;
import com.example.common.exception.BusinessException;
import com.example.common.util.AjaxResultUtil;
import com.example.common.util.RedisUtil;
import com.example.common.util.SnowflakeIdWorker;
import com.example.goodservice.client.OrderClient;
import com.example.goodservice.client.UserClient;
import com.example.goodservice.dto.AddReviewDTO;
import com.example.goodservice.dto.OrderDTO;
import com.example.goodservice.dto.UserDTO;
import com.example.goodservice.entity.Goods;
import com.example.goodservice.entity.ProductReview;
import com.example.goodservice.entity.StockLog;
import com.example.goodservice.mapper.GoodsMapper;
import com.example.goodservice.service.GoodService;
import com.example.goodservice.vo.AddReviewVO;
import com.example.goodservice.vo.ReviewVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.apache.catalina.User;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private static final Logger log = LoggerFactory.getLogger(GoodServiceImpl.class);
    @Autowired
    public GoodsMapper goodsMapper;

    @Autowired
    public RedisUtil redisUtil;
    
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderClient orderClient;

    @Autowired
    SnowflakeIdWorker snowflakeIdWorker;

    @Autowired
    private UserClient userClient;

    @Autowired
    private TransactionTemplate transactionTemplate;

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

    /**
     * 添加商品评论
     * <p>
     * 业务流程：
     * 1、参数校验：订单号、商品ID、评分范围（1-5）、内容长度（≤500字）
     * 2、调用订单服务验证订单信息（订单是否存在、是否属于该用户、是否已支付）
     * 3、验证订单中是否包含该商品
     * 4、检查商品是否已评价（防止重复评价）
     * 5、生成评论编号（雪花算法）
     * 6、插入评论记录，同时更新商品评分（事务保证一致性）
     * 7、删除商品缓存，确保下次查询获取最新评分
     * 8、返回评论信息
     * </p>
     *
     * @param userId        用户ID
     * @param username      用户名（未使用，保留参数）
     * @param addReviewDTO  评论信息DTO
     * @return 评论结果VO
     * @throws BusinessException 参数校验失败、订单验证失败、重复评价时抛出
     */
    @Override
    public AddReviewVO addReview(Long userId, String username, AddReviewDTO addReviewDTO) {
        if (addReviewDTO.getOrderNo() == null){
            throw new BusinessException("订单号不能为空");
        }
        if (addReviewDTO.getProductId() == null){
            throw new BusinessException("商品订单不能为空");
        }
        if (addReviewDTO.getRating()<1 || addReviewDTO.getRating()>5){
            throw new BusinessException("评分不合法");
        }
        if(addReviewDTO.getContent()!=null && addReviewDTO.getContent().length() > 500){
            throw new BusinessException("评论内容不得大于500字");
        }
        OrderDTO orderDTO = AjaxResultUtil.getData(orderClient.orderReview(addReviewDTO.getOrderNo()), OrderDTO.class);
        if(orderDTO == null){
            throw new BusinessException("订单信息不存在");
        }
        if (orderDTO.getUserId() == null || orderDTO.getProductId() == null || orderDTO.getStatus() == null) {
            throw new BusinessException("订单数据异常，请联系客服");
        }
        if (!orderDTO.getUserId().equals(userId)){
            throw new BusinessException("无权评论该订单");
        }
        if (orderDTO.getStatus() == 0){
            throw new BusinessException("订单未支付");
        }
        if (!orderDTO.getProductId().contains(addReviewDTO.getProductId())) {
            throw new BusinessException("该商品不在订单中");
        }
        int count = goodsMapper.reviewPlagiarismCheck(addReviewDTO.getOrderNo(), addReviewDTO.getProductId());
        if (count>0){
            throw new BusinessException("商品已评价");
        }
        long id = snowflakeIdWorker.nextId();
        String reviewNo = String.valueOf(id);
//        addReviewDTO.setReviewNo(reviewNo);
//        addReviewDTO.setUserId(userId);
        ProductReview review = new ProductReview();
        review.setReviewNo(reviewNo);
        review.setUserId(userId);
        review.setProductId(addReviewDTO.getProductId());
        review.setOrderNo(addReviewDTO.getOrderNo());
        review.setRating(addReviewDTO.getRating());
        review.setContent(addReviewDTO.getContent());
        review.setPics(addReviewDTO.getPics());
        Date now = new Date();
        review.setCreateTime(now);
        transactionTemplate.execute(status -> {
            goodsMapper.insertReview(review);
            goodsMapper.updateReview(addReviewDTO.getRating(),addReviewDTO.getProductId());
            return null;
        });
        redisUtil.delete(GOODS_CACHE_PREFIX + addReviewDTO.getProductId());
        AddReviewVO vo = new AddReviewVO();
        vo.setReviewNo(reviewNo);
        vo.setProductId(addReviewDTO.getProductId());
        vo.setRating(addReviewDTO.getRating());
        vo.setContent(addReviewDTO.getContent());
        vo.setPics(addReviewDTO.getPics());
        vo.setCreateTime(now);
        return vo;
    }
    /**
     * 分页查询商品评论列表
     * <p>
     * 业务流程：
     * 1、参数校验和默认值处理（页码≥1，每页≥10条，排序字段只能是 create_time 或 rating）
     * 2、查询评论列表（使用 PageHelper 分页）
     * 3、如果评论为空，直接返回空分页结果
     * 4、获取评论列表中的所有用户ID
     * 5、调用用户服务批量查询用户信息
     * 6、关联用户信息组装 ReviewVO（包含用户名）
     * 7、设置分页总数，返回分页结果
     * </p>
     *
     * @param productId  商品ID
     * @param page       页码（默认1）
     * @param pageSize   每页数量（默认10）
     * @param sort       排序字段（create_time 或 rating，默认 create_time）
     * @return 分页评论结果
     * @throws BusinessException 商品ID为空或用户服务调用失败时抛出
     */
    @SuppressWarnings("resource")
    @Override
    public PageInfo<ReviewVO> review(Long productId, Integer page, Integer pageSize, String sort) {
        if (productId == null){
            throw new BusinessException("商品ID不能为空");
        }
        if (page<1){
            page = 1;
        }
        if (pageSize < 10){
            pageSize = 10;
        }
        if (!sort.equals("create_time")&&!sort.equals("rating")){
            sort = "create_time";
        }
        PageHelper.startPage(page,pageSize);
        List<ProductReview> reviews = goodsMapper.review(productId,sort);
        long total = ((com.github.pagehelper.Page<?>)reviews).getTotal();
        if (total == 0){
            return new PageInfo<>(Collections.emptyList());
        }
        //获取列表中的用户id
        List<Long> userIds = reviews.stream()
                .map(ProductReview::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UserDTO> userMap = new HashMap<>();
        AjaxResult result = userClient.reviewUserInfo(userIds);
        if (result == null || !result.isSuccess()){
            throw new BusinessException("未查询到用户信息");
        }
        List<UserDTO> userList = AjaxResultUtil.getDataList(result, UserDTO.class);
        for (UserDTO user : userList){
            userMap.put(user.getId(),user);
        }
        List<ReviewVO> reviewVOS = new ArrayList<>();
        for (ProductReview review : reviews) {
            ReviewVO vo = new ReviewVO();
            vo.setReviewNo(review.getReviewNo());
            vo.setUserId(review.getUserId());
            vo.setRating(review.getRating());
            vo.setContent(review.getContent());
            vo.setCreateTime(review.getCreateTime());
            String picsStr = review.getPics();
            if (picsStr != null && !picsStr.isEmpty()){
                vo.setPics(Arrays.asList(picsStr.split(",")));
            }else{
                vo.setPics(new ArrayList<>());
            }
            UserDTO user = userMap.get(review.getUserId());
            if (user != null){
                vo.setUsername(user.getUsername());
            }else {
                vo.setUsername("");
            }
            reviewVOS.add(vo);
        }
        PageInfo<ReviewVO> pageInfo = new PageInfo<>(reviewVOS);
        pageInfo.setTotal(total);
        return pageInfo;
    }

    /**
     * 删除商品评论
     * <p>
     * 业务流程：
     * 1、根据评论编号查询评论信息
     * 2、验证评论是否存在
     * 3、验证用户是否有权删除该评论（评论所属用户必须与当前用户一致）
     * 4、删除评论记录，同时回滚商品评分（事务保证一致性）
     * 5、删除商品缓存，确保下次查询获取最新评分
     * </p>
     *
     * @param userId    用户ID
     * @param reviewNo  评论编号
     * @throws BusinessException 评论不存在或无权删除时抛出
     */
    @Override
    public void delReview(Long userId, String reviewNo) {
        ProductReview review = goodsMapper.reviewInfoByNo(reviewNo);
        if (review == null){
            throw new BusinessException("评论不存在");
        }
        if (review.getUserId() != userId){
            throw new BusinessException("无权删除该评论");
        }
        transactionTemplate.execute(status -> {
            goodsMapper.delReview(reviewNo);
            goodsMapper.rollbackReview(review.getProductId(),review.getRating());
            return null;
        });
        redisUtil.delete(GOODS_CACHE_PREFIX+review.getProductId());
    }


}
