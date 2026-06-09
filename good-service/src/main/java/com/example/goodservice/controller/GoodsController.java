package com.example.goodservice.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.common.domain.AjaxResult;
import com.example.common.exception.BusinessException;
import com.example.goodservice.dto.AddReviewDTO;
import com.example.goodservice.dto.OrderDTO;
import com.example.goodservice.entity.Goods;
import com.example.goodservice.mapper.GoodsMapper;
import com.example.goodservice.service.GoodService;
import com.example.goodservice.vo.AddReviewVO;
import com.example.goodservice.vo.ReviewVO;
import com.github.pagehelper.PageInfo;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品服务 REST 控制器
 * <p>
 * 提供商品信息查询、库存扣减、库存恢复等功能
 * 使用 Redis 缓存商品信息，并使用 Sentinel 对扣库存接口进行限流保护
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
@RestController
@RequestMapping("/good")
public class GoodsController {
    @Autowired
    private GoodService goodService;

    @Autowired
    private GoodsMapper goodsMapper;

    /**
     * 根据商品 ID 查询商品信息
     *
     * @param id 商品ID
     * @return 商品信息
     */
    @GetMapping("/goodInfo")
    public ResponseEntity<AjaxResult> goodInfoById(@RequestParam Long id) {
        try {
            Goods goods = goodService.goodInfo(id);
            return ResponseEntity.ok(AjaxResult.success(goods));
        } catch (BusinessException e) {
            return ResponseEntity.status(400).body(AjaxResult.error(e.getMessage()));
        }
    }

    /**
     * 扣减商品库存 （支持限流）
     * <p>
     * 使用 Sentinel 限流，资源名"reduceStock",OPS 超过阈值时触发降级
     * </p>
     *
     * @param id        商品ID
     * @param quantity  扣减数量
     * @return 操作结果
     */
    @PostMapping("/reduceStock")
    @SentinelResource(value = "reduceStock", blockHandler = "handleBlock")
    public ResponseEntity<AjaxResult> reduceStock(@RequestParam Long id,
                                                  @RequestParam Integer quantity) {
        try {
            goodService.reduceStock(id, quantity);
            return ResponseEntity.ok(AjaxResult.success("库存扣减成功"));
        }catch (BusinessException e){
            return ResponseEntity.status(400).body(AjaxResult.error(e.getMessage()));
        }
    }

    /**
     * 扣库存接口限流降级处理器
     *
     * @param id        商品ID
     * @param quantity  扣减数量
     * @param ex        Sentinel 限流异常
     * @return 限流提示响应（HTTP 429）
     */
    public ResponseEntity<AjaxResult> handleBlock(Long id,
                                                  Integer quantity,
                                                  BlockException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(AjaxResult.error("系统繁忙，请稍后再试"));
    }

    /**
     * 恢复商品库存（用于订单超时取消等场景）
     *
     * @param id        商品ID
     * @param quantity  恢复数量
     * @return 操作结果
     */
    @PostMapping("increaseStock")
    public ResponseEntity<AjaxResult> increaseStock(@RequestParam Long id,
                                                    @RequestParam Long quantity) {
        int rows = goodsMapper.increaseStock(id,quantity);
        if (rows == 0) {
            return ResponseEntity.status(400).body(AjaxResult.error("库存恢复失败"));
        }
        return ResponseEntity.ok(AjaxResult.success("库存恢复成功"));
    }

    /**
     * 添加商品评论
     * <p>
     * 用户对已购买的商品进行评论，需验证用户身份和订单信息
     * </p>
     *
     * @param userId        用户ID（从请求头获取）
     * @param username      用户名（从请求头获取）
     * @param addReviewDTO  评论信息（订单号、商品ID、评分、内容、图片）
     * @return 操作结果
     */
    @PostMapping("addReview")
    public ResponseEntity<AjaxResult> addReview(@RequestHeader("X-User-Id") Long userId,
                                                @RequestHeader("X-User-Name") String username,
                                                @RequestBody AddReviewDTO addReviewDTO){
        try {
            AddReviewVO vo = goodService.addReview(userId,username,addReviewDTO);
            return ResponseEntity.ok(AjaxResult.success(vo));
        }catch (BusinessException e){
            return ResponseEntity.status(400).body(AjaxResult.error(e.getMessage()));
        }
    }

    /**
     * 分页查询商品评论列表
     * <p>
     * 获取指定商品的所有评论，支持分页和排序
     * </p>
     *
     * @param productId  商品ID
     * @param page       页码（默认1）
     * @param pageSize   每页数量（默认10）
     * @param sort       排序字段（create_time 或 rating，默认 create_time）
     * @return 分页评论列表
     */
    @PostMapping("review")
    public ResponseEntity<AjaxResult> Review(@RequestParam Long productId,
                                             @RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                             @RequestParam(defaultValue = "create_time") String sort){
        try {
            return ResponseEntity.ok(AjaxResult.success(goodService.review(productId,page,pageSize,sort)));
        }catch (Exception e){
            return ResponseEntity.status(400).body(AjaxResult.error(e.getMessage()));
        }
    }
    /**
     * 删除商品评论
     * <p>
     * 用户删除自己的评论，需验证用户身份和评论所有权
     * </p>
     *
     * @param userId    用户ID（从请求头获取）
     * @param reviewNo  评论编号
     * @return 操作结果
     */
    @PostMapping("delReview")
    public ResponseEntity<AjaxResult> delReview(@RequestHeader("X-User-Id")Long userId,
                                                @RequestParam String reviewNo){
        try {
            goodService.delReview(userId,reviewNo);
            return ResponseEntity.ok(AjaxResult.success("评论删除成功"));
        }catch (Exception e){
            return ResponseEntity.status(400).body(AjaxResult.error(e.getMessage()));
        }
    }
}
