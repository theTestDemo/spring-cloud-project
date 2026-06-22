package com.example.ai.dto;

/**
 * 带工具调用的多轮对话请求体
 * <p>
 * 用于 {@code /ai/chatWithTools} 接口，封装会话标识和用户消息。
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-06-19
 */
public class ChatRequest {

    /** 会话 ID，用于区分不同用户的对话上下文 */
    private String sessionId = "default";

    /** 用户消息 */
    private String message;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
