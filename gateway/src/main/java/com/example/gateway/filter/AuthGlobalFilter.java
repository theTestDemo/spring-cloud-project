package com.example.gateway.filter;

import com.example.common.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关全局认证过滤器
 * <p>
 * 拦截所有请求，校验 JWT Token 的有效性
 * 对于公开接口（如登录、注册）直接放行；
 * 其他接口需要请求头中包含有效的 Authorization:Bearer <token>
 * 校验通过后将用户名放入请求头 X-User-Name 传递给下游服务
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    /**
     * 过滤器核心逻辑
     *
     * @param exchange  请求上下文
     * @param chain     过滤器链
     * @return 继续过滤或直接返回响应
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        String path = request.getURI().getPath();

        // 放行登录、注册等公开接口
        if (path.contains("/user/login") || path.contains("/user/register")) {
            return chain.filter(exchange);
        }

        // 获取 Authorization 头
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        String token = authHeader.substring(7);
        if (!JwtUtil.validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        //将用户信息放入请求头，传递给下游服务
        String username = JwtUtil.getUsernameFromToken(token);
        Long userId = JwtUtil.getUserIdFromToken(token);
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Name", username)
                .header("X-User-Id", String.valueOf(userId))
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    /**
     * 设置过滤器优先级(数值越小越优先)
     * @return 优先顺序
     */
    @Override
    public int getOrder() {
        return -1; // 最高优先级
    }
}