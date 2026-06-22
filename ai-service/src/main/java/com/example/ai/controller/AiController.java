package com.example.ai.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.ai.dto.ChatRequest;
import com.example.ai.service.AiService;
import com.example.ai.service.StreamingAiService;
import com.example.common.domain.AjaxResult;
import dev.langchain4j.service.TokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话 REST 控制器
 * <p>
 * 提供普通对话、工具对话、流式对话三种 API 接口。
 * 所有接口均接入 Sentinel 限流，防止大模型 API 调用费用失控。
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-06-19
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    /** SSE 连接超时时间（毫秒），覆盖一次完整流式对话的最大时长 */
    private static final long SSE_TIMEOUT_MS = 300_000L;

    private final AiService aiService;
    private final StreamingAiService streamingAiService;

    public AiController(AiService aiService,
                        StreamingAiService streamingAiService) {
        this.aiService = aiService;
        this.streamingAiService = streamingAiService;
    }

    /**
     * 普通对话
     * <p>
     * 用户发送消息，AI 返回完整回复（同步）。
     * AI 可根据用户意图自动调用内部微服务工具（查询订单、查询评论），
     * 并将工具返回的数据整合为自然语言回复。
     * </p>
     *
     * @param userMessage 用户消息文本
     * @return AI 回复内容
     */
    @PostMapping("/chat")
    @SentinelResource(value = "aiChat", blockHandler = "chatBlockHandler")
    public ResponseEntity<AjaxResult> chat(@RequestBody String userMessage) {
        String reply = aiService.chat(userMessage);
        return ResponseEntity.ok(AjaxResult.success(reply));
    }

    /**
     * 普通对话限流降级处理
     * <p>
     * 当 {@code chat} 接口触发 Sentinel 限流规则（QPS > 10）时调用此方法，
     * 返回友好提示，避免直接抛出异常。
     * 限流规则在 {@link com.example.ai.config.SentinelConfig} 中配置，资源名为 {@code aiChat}
     * </p>
     *
     * @param userMessage 用户消息（与原方法参数一致）
     * @param ex          Sentinel 限流异常，包含触发降级的原因
     * @return 统一的限流响应，HTTP 状态码为 429（Too Many Requests）
     */
    public ResponseEntity<AjaxResult> chatBlockHandler(String userMessage, BlockException ex) {
        return ResponseEntity.status(429).body(AjaxResult.error("当前对话请求过多，请稍后再试"));
    }

    /**
     * 带工具调用的多轮对话
     * <p>
     * 业务流程：
     * 1、根据 sessionId 加载历史对话上下文
     * 2、将用户消息发送给大模型（携带工具定义）
     * 3、若模型判断需要调用工具，自动执行并将结果回传模型
     * 4、循环直到模型不再调用工具，返回最终文本回复
     * 5、将本轮对话保存到记忆（按 sessionId 隔离）
     * </p>
     *
     * @param request 包含 sessionId（会话标识）和 message（用户消息）
     * @return AI 回复内容（可能包含工具调用结果的整合回复）
     */
    @PostMapping("/chatWithTools")
    @SentinelResource(value = "aiChatWithTools", blockHandler = "chatWithToolsBlockHandler")
    public ResponseEntity<AjaxResult> chatWithTools(@RequestBody ChatRequest request) {
        String reply = aiService.chatWithTools(request.getSessionId(), request.getMessage());
        return ResponseEntity.ok(AjaxResult.success(reply));
    }

    /**
     * 工具对话限流降级处理
     * <p>
     * 当 {@code chatWithTools} 接口触发 Sentinel 限流规则（QPS > 5）时调用此方法。
     * 工具对话涉及跨服务调用（订单/商品），耗时更长，因此限流阈值更低。
     * </p>
     *
     * @param request 请求体（与原方法参数一致）
     * @param ex      Sentinel 限流异常
     * @return 统一的限流响应，HTTP 状态码为 429
     */
    public ResponseEntity<AjaxResult> chatWithToolsBlockHandler(ChatRequest request, BlockException ex) {
        return ResponseEntity.status(429).body(AjaxResult.error("当前对话请求过多，请稍后再试"));
    }

    /**
     * 流式对话（SSE 逐字输出）
     * <p>
     * 使用 {@link SseEmitter} 实现异步 SSE 推送，解决直接写 {@code HttpServletResponse}
     * 时 Servlet 方法返回后 Tomcat 立即回收连接导致 {@code RecycleRequiredException} 的问题。
     * </p>
     * <p>
     * 注意：LangChain4j 目前流式模式不支持工具调用（Function Calling），
     * 此接口仅提供普通的流式多轮对话，不支持自动调用内部微服务工具。
     * 若需使用工具调用，请使用 {@link #chatWithTools} 接口。
     * </p>
     * <p>
     * 前端使用 EventSource 接收逐字输出：
     * <pre>
     * const eventSource = new EventSource('/ai/chat/stream?sessionId=s1&amp;message=你好');
     * eventSource.onmessage = (event) =&gt; console.log(event.data);
     * </pre>
     * 每个 token 以 SSE 格式（{@code data: xxx}）推送，结束时发送 {@code data: [DONE]}。
     * </p>
     *
     * @param sessionId 会话 ID（默认 "default"，用于隔离不同用户的对话上下文）
     * @param message   用户消息
     * @return SseEmitter，由 Spring MVC 异步框架持有连接，直到 complete/error 才关闭
     */
    @GetMapping("/chat/stream")
    @SentinelResource(value = "aiChatStream", blockHandler = "chatStreamBlockHandler")
    public SseEmitter chatStream(
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestParam String message) {

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        TokenStream tokenStream = streamingAiService.chatStream(sessionId, message);

        tokenStream
                .onNext(token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        log.warn("SSE 发送 token 失败，客户端可能已断开连接", e);
                    }
                })
                .onComplete(aiMessage -> {
                    try {
                        // 必须先发送 [DONE] 信号，通知前端流已正常结束，
                        // 否则 emitter.complete() 关闭连接后前端 EventSource 会触发 onerror
                        emitter.send(SseEmitter.event().data("[DONE]"));
                    } catch (Exception e) {
                        log.warn("SSE 发送完成信号失败", e);
                    }
                    emitter.complete();
                })
                .onError(throwable -> {
                    String errorMsg = throwable.getMessage() != null
                            ? throwable.getMessage()
                            : "未知错误";
                    try {
                        emitter.send(SseEmitter.event().data("错误：" + errorMsg));
                        emitter.send(SseEmitter.event().data("[DONE]"));
                    } catch (Exception e) {
                        log.warn("SSE 发送错误信息失败", e);
                    }
                    emitter.complete();
                })
                .start();

        return emitter;
    }

    /**
     * 流式对话限流降级处理
     * <p>
     * 当 {@code chatStream} 接口触发 Sentinel 限流规则（QPS > 5）时调用此方法。
     * 流式对话为长连接，占用资源较多，因此限流阈值与工具对话相同。
     * </p>
     *
     * @param sessionId 会话 ID（与原方法参数一致）
     * @param message   用户消息（与原方法参数一致）
     * @param ex        Sentinel 限流异常
     * @return SseEmitter，发送限流提示后立即关闭
     */
    public SseEmitter chatStreamBlockHandler(String sessionId, String message, BlockException ex) {
        SseEmitter emitter = new SseEmitter();
        try {
            emitter.send(SseEmitter.event().data("当前对话请求过多，请稍后再试"));
            emitter.send(SseEmitter.event().data("[DONE]"));
        } catch (Exception e) {
            log.warn("SSE 发送限流提示失败", e);
        }
        emitter.complete();
        return emitter;
    }
}
