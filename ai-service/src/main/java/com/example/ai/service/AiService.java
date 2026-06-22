package com.example.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI 同步对话服务接口
 * <p>
 * 提供普通对话和带工具调用的多轮对话两种同步模式。
 * 流式对话通过 {@link StreamingAiService} 提供。
 * </p>
 * <p>
 * 注意：{@link SystemMessage}、{@link UserMessage}、{@link MemoryId} 注解必须放在接口上，
 * LangChain4j 通过动态代理读取这些注解来构建调用逻辑。
 * 工具定义见 {@link com.example.ai.tools.AiToolProvider}。
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-06-19
 */
public interface AiService {

    /**
     * 普通对话（无会话记忆，单轮对话）
     *
     * @param userMessage 用户消息
     * @return AI 回复
     */
    @SystemMessage("你是一个智能客服助手，服务于电商平台的用户。"
            + "你可以通过工具查询用户的订单信息和商品评论。"
            + "请遵循以下规则："
            + "1. 当用户询问订单相关问题时，使用 getOrderInfo 工具查询订单详情"
            + "2. 当用户询问商品评价时，使用 getReviewList 工具查看评论"
            + "3. 将查询结果用友好、简洁的自然语言回复用户"
            + "4. 如果工具调用失败，礼貌地告知用户并建议稍后重试"
            + "5. 对于无法处理的问题，建议用户联系人工客服")
    String chat(@UserMessage String userMessage);

    /**
     * 带工具调用的多轮对话
     * <p>
     * 业务流程：
     * 1、根据 sessionId 加载历史对话上下文
     * 2、将系统提示词 + 历史消息 + 用户消息 + 工具定义发送给大模型
     * 3、若模型返回 tool_calls，LangChain4j 自动执行工具并将结果回传
     * 4、循环直到模型不再调用工具，返回最终文本回复
     * 5、将本轮对话保存到记忆（按 sessionId 隔离，最多保留 20 条）
     * </p>
     *
     * @param sessionId 会话 ID（用于隔离不同用户/会话的上下文）
     * @param message   用户消息
     * @return AI 回复
     */
    @SystemMessage("你是一个智能客服助手，服务于电商平台的用户。"
            + "你可以通过工具查询用户的订单信息和商品评论。"
            + "请遵循以下规则："
            + "1. 当用户询问订单相关问题时，使用 getOrderInfo 工具查询订单详情"
            + "2. 当用户询问商品评价时，使用 getReviewList 工具查看评论"
            + "3. 将查询结果用友好、简洁的自然语言回复用户"
            + "4. 如果工具调用失败，礼貌地告知用户并建议稍后重试"
            + "5. 对于无法处理的问题，建议用户联系人工客服")
    String chatWithTools(@MemoryId String sessionId,
                         @UserMessage String message);
}
