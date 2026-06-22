package com.example.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * AI 流式对话服务接口
 * <p>
 * 基于 LangChain4j 的 AI 服务接口，通过 {@code AiServices.builder()} 由框架自动注入
 * 流式聊天模型和记忆管理。返回 {@link TokenStream} 供 Controller 层
 * 实现 SSE 逐 token 推送。
 * </p>
 * <p>
 * 注意：LangChain4j 目前流式模式不支持工具调用（Function Calling），
 * 此接口仅提供普通的流式多轮对话。若需使用工具调用，请使用同步的 {@link AiService}。
 * 多轮对话记忆通过 {@link dev.langchain4j.memory.chat.ChatMemoryProvider} 按 memoryId 隔离维护。
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-06-19
 */
public interface StreamingAiService {

    /**
     * 流式对话（SSE 逐字输出）
     * <p>
     * 返回 {@link TokenStream}，调用方可通过 {@code onNext}、{@code onComplete}、
     * {@code onError} 回调逐 token 处理 AI 输出，实现流式响应。
     * </p>
     * <p>
     * 注意：此方法不支持工具调用（Function Calling），仅提供普通对话流式输出。
     * 多轮对话上下文通过 memoryId 维护。
     * </p>
     *
     * @param memoryId 会话 ID，用于隔离不同用户的对话上下文
     * @param message  用户消息文本
     * @return TokenStream 流式响应，支持逐 token 回调
     */
    @SystemMessage("你是一个智能客服助手，服务于电商平台的用户。"
            + "请用友好、简洁的自然语言回复用户。")
    TokenStream chatStream(@MemoryId String memoryId, @UserMessage String message);
}
