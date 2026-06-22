package com.example.ai.service.impl;

import com.example.ai.service.AiService;

/**
 * AI 对话服务实现占位类
 * <p>
 * 实际运行时，{@link AiService} 的实现由 LangChain4j 的 {@code AiServices.builder()} 动态代理生成，
 * 不需要手写实现。此类保留仅为符合项目分层结构约定。
 * </p>
 * <p>
 * 工具定义见 {@link com.example.ai.tools.AiToolProvider}，
 * 流式服务接口见 {@link com.example.ai.service.StreamingAiService}。
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-06-19
 */
public class AiServiceImpl implements AiService {

    @Override
    public String chat(String userMessage) {
        return chatWithTools("default", userMessage);
    }

    @Override
    public String chatWithTools(String sessionId, String message) {
        throw new UnsupportedOperationException("由 LangChain4j 动态代理实现");
    }
}
