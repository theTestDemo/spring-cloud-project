package com.example.ai.tools;

import com.alibaba.fastjson.JSON;
import com.example.ai.client.GoodClient;
import com.example.ai.client.OrderClient;
import com.example.common.domain.AjaxResult;
import com.example.common.util.AjaxResultUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AI 工具提供者
 * <p>
 * 封装了订单查询和评论查询两个工具，作为独立 Bean 被同步和流式两个 AI Service 共享。
 * LangChain4j 会自动扫描 {@link Tool} 注解方法，构建工具定义发给大模型，
 * 当大模型判断需要调用工具时，自动执行对应方法并将结果回传给模型。
 * </p>
 * <p>
 * 每个工具方法通过 {@link Tool} 注解描述功能，通过 {@link P} 注解描述参数含义，
 * 帮助大模型理解何时调用、如何传参。
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-06-19
 */
@Component
public class AiToolProvider {

    private static final Logger log = LoggerFactory.getLogger(AiToolProvider.class);

    private final OrderClient orderClient;
    private final GoodClient goodClient;

    public AiToolProvider(OrderClient orderClient, GoodClient goodClient) {
        this.orderClient = orderClient;
        this.goodClient = goodClient;
    }

    /**
     * 工具：根据订单号查询订单详情
     * <p>
     * 通过 Feign 调用 order-service 的 {@code /order/orderInfoByOrderNo} 接口，
     * 返回订单的完整信息（状态、金额、商品列表等）。
     * </p>
     *
     * @param orderNo 订单号
     * @return 订单信息的 JSON 字符串，或错误提示
     */
    @Tool("根据订单号查询订单详细信息，包括订单状态、用户ID、金额、商品等")
    public String getOrderInfo(@P("订单号") String orderNo) {
        log.info("[Tool] 调用 getOrderInfo, orderNo={}", orderNo);
        try {
            AjaxResult result = orderClient.orderInfoByOrderNo(orderNo);
            if (result != null && result.isSuccess()) {
                Object data = AjaxResultUtil.getData(result, Object.class);
                return JSON.toJSONString(data);
            }
            return "订单查询失败：" + (result != null ? result.get("msg") : "服务不可用");
        } catch (Exception e) {
            log.error("[Tool] getOrderInfo 异常, orderNo={}", orderNo, e);
            return "订单服务异常：" + e.getMessage();
        }
    }

    /**
     * 工具：根据商品ID查询评论列表
     * <p>
     * 通过 Feign 调用 good-service 的 {@code /good/review} 接口，
     * 返回评论列表（包含评论内容、评分、用户信息等），默认按创建时间倒序，取前 5 条。
     * </p>
     *
     * @param productId 商品 ID
     * @return 评论列表的 JSON 字符串，或错误提示
     */
    @Tool("根据商品ID查询评论列表，包含评论内容、评分、用户信息等")
    public String getReviewList(@P("商品ID") Long productId) {
        log.info("[Tool] 调用 getReviewList, productId={}", productId);
        try {
            AjaxResult result = goodClient.review(productId, 1, 5, "create_time");
            if (result != null && result.isSuccess()) {
                Object data = AjaxResultUtil.getData(result, Object.class);
                return JSON.toJSONString(data);
            }
            return "评论查询失败：" + (result != null ? result.get("msg") : "服务不可用");
        } catch (Exception e) {
            log.error("[Tool] getReviewList 异常, productId={}", productId, e);
            return "评论服务异常：" + e.getMessage();
        }
    }
}
