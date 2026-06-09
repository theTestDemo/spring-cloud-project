package com.example.goodservice.client;

import com.example.common.domain.AjaxResult;
import com.example.goodservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户服务 Feign 客户端
 * <p>
 * 用于批量查询用户信息，提供评论列表中用户名称的关联展示
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
@FeignClient(name = "user-service")
public interface UserClient {
    /**
     * 批量查询用户信息（用于商品评论展示）
     * <p>
     * 根据用户ID列表批量获取用户信息，用于评论列表中展示评论者用户名
     * </p>
     *
     * @param userIds 用户ID列表
     * @return 用户信息列表结果
     */
    @PostMapping("user/reviewUserInfo")
    public AjaxResult reviewUserInfo(@RequestBody List<Long> userIds);
}
