package com.example.ai.config;

import com.example.ai.service.AiService;
import com.example.ai.service.StreamingAiService;
import com.example.ai.tools.AiToolProvider;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j AI 配置类
 * <p>
 * 通过 DashScope 原生 SDK 对接阿里百炼大模型（非 OpenAI 兼容层）。
 * 同步模式完整支持 Function Calling；流式模式仅支持普通对话（不支持工具调用）。
 * </p>
 * <p>
 * 配置内容：
 * 1、同步聊天模型（用于普通对话和工具对话）
 * 2、流式聊天模型（用于 SSE 逐字输出，仅支持普通对话）
 * 3、对话记忆提供者（同步和流式共享，按 sessionId 隔离，最多保留 20 条消息）
 * 4、AI 服务代理（同步含工具调用 + 流式仅普通对话，均支持多轮对话记忆）
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-06-19
 */
@Configuration
public class AiConfig {

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model}")
    private String model;

    /** 对话记忆最大消息数，防止 token 爆炸 */
    private static final int MAX_MEMORY_MESSAGES = 20;

    /**
     * 同步聊天模型
     * <p>
     * 使用 DashScope 原生 SDK 调用通义千问大模型（非 OpenAI 兼容层）。
     * 原生 SDK 对工具调用（Function Calling）的支持更稳定。
     * 用于普通对话（{@code /ai/chat}）和工具对话（{@code /ai/chatWithTools}）。
     * </p>
     *
     * @return 同步聊天模型实例
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .build();
    }

    /**
     * 流式聊天模型
     * <p>
     * 使用 DashScope 原生 SDK 的流式模型，用于 SSE 逐字输出。
     * 注意：LangChain4j 流式模式不支持工具调用（Function Calling），
     * 此模型仅用于普通流式对话（{@code /ai/chat/stream}）。
     * </p>
     *
     * @return 流式聊天模型实例
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .build();
    }

    /**
     * 对话记忆提供者
     * <p>
     * 同步和流式两个 AI 服务共享，按 memoryId（即 sessionId）隔离不同会话。
     * 每个会话使用消息窗口策略，最多保留 {@value #MAX_MEMORY_MESSAGES} 条历史消息。
     * </p>
     *
     * @return ChatMemoryProvider 实例
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.withMaxMessages(MAX_MEMORY_MESSAGES);
    }

    /**
     * 同步 AI 服务代理
     * <p>
     * 通过 {@link AiServices#builder} 动态代理生成，自动处理：
     * <ul>
     *     <li>Function Calling 的完整循环（注册工具 → 模型决策 → 执行 → 结果回传 → 循环直到不再调用工具）</li>
     *     <li>多轮对话记忆管理（按 sessionId 隔离）</li>
     *     <li>消息窗口裁剪（防止 token 爆炸）</li>
     * </ul>
     * </p>
     *
     * @param chatModel        同步聊天模型
     * @param toolProvider     工具提供者
     * @param memoryProvider   对话记忆提供者
     * @return AI 服务代理实例
     */
    @Bean
    public AiService aiService(ChatLanguageModel chatModel,
                               AiToolProvider toolProvider,
                               ChatMemoryProvider memoryProvider) {
        return AiServices.builder(AiService.class)
                .chatLanguageModel(chatModel)
                .tools(toolProvider)
                .chatMemoryProvider(memoryProvider)
                .build();
    }

    /**
     * 流式 AI 服务代理
     * <p>
     * 与同步服务类似，但使用流式模型，返回 {@link dev.langchain4j.service.TokenStream}
     * 供 Controller 逐 token 写入 SSE 响应流。
     * </p>
     * <p>
     * 注意：LangChain4j 在流式模式下不支持工具调用（Function Calling），
     * 因此此处不配置 tools。若需使用工具调用，请使用同步的 {@link AiService}。
     * 多轮对话记忆通过 {@link ChatMemoryProvider} 按 memoryId 隔离维护。
     * </p>
     *
     * @param streamingModel   流式聊天模型
     * @param memoryProvider   对话记忆提供者
     * @return 流式 AI 服务代理实例
     */
    @Bean
    public StreamingAiService streamingAiService(
            StreamingChatLanguageModel streamingModel,
            ChatMemoryProvider memoryProvider) {

        return AiServices.builder(StreamingAiService.class)
                .streamingChatLanguageModel(streamingModel)
                .chatMemoryProvider(memoryProvider)
                .build();
    }
}
