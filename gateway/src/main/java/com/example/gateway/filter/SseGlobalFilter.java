package com.example.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * SSE 流式响应全局过滤器
 * <p>
 * 解决问题：Spring Cloud Gateway（基于 Netty/WebFlux）在转发下游响应时，
 * 可能因压缩、缓存等机制缓冲 SSE 数据块，导致浏览器无法实时收到每个 token。
 * </p>
 * <p>
 * 处理逻辑：通过 {@code beforeCommit} 钩子，在响应提交（发送 headers）前检测 Content-Type，
 * 若为 {@code text/event-stream}，则：
 * <ul>
 *   <li>设置 {@code Content-Encoding: identity} —— 禁用压缩，避免 Netty 攒批发送</li>
 *   <li>设置 {@code X-Accel-Buffering: no} —— 防止 Nginx 等反向代理缓冲</li>
 *   <li>设置 {@code Cache-Control: no-cache} —— 防止客户端/中间层缓存</li>
 * </ul>
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-06-19
 */
@Component
public class SseGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 在响应提交前（headers 发出前）注册回调，修改 SSE 相关响应头
        exchange.getResponse().beforeCommit(response -> {
            HttpHeaders headers = response.getHeaders();
            MediaType contentType = headers.getContentType();
            boolean isSse = contentType != null
                    && "text".equalsIgnoreCase(contentType.getType())
                    && "event-stream".equalsIgnoreCase(contentType.getSubtype());

            if (isSse) {
                // 禁用压缩，确保每个 SSE 数据块立即 flush
                headers.set("Content-Encoding", "identity");
                // 防止 Nginx 等反向代理缓冲 SSE 响应
                headers.set("X-Accel-Buffering", "no");
                // 防止客户端或中间层缓存
                headers.set("Cache-Control", "no-cache");
            }
            return Mono.empty();
        });

        return chain.filter(exchange);
    }

    /**
     * 优先级设为最低（数值最大），确保在 AuthGlobalFilter 等业务过滤器之后执行。
     * beforeCommit 回调不受 order 影响，只要响应提交前注册即可生效。
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
