package com.example.goodservice.fallback;

import com.example.common.domain.AjaxResult;
import com.example.goodservice.client.UserClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * UserClient 熔断降级工厂
 * <p>
 * 当 user-service 服务不可用时，返回降级响应，避免级联故障
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {
    private static final Logger log = LoggerFactory.getLogger(UserClientFallbackFactory.class);

    @Override
    public UserClient create(Throwable cause) {
        log.error("user-service 调用失败: {}", cause.getMessage());
        return new UserClient() {
            @Override
            public AjaxResult reviewUserInfo(java.util.List<Long> userIds) {
                return AjaxResult.error("获取用户信息失败");
            }
        };
    }
}
