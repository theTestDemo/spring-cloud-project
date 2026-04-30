package com.example.common.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;
/**
 * Redis 工具类
 * <p>
 * 封装了常用的 Redis 字符串操作（存、取、删）。
 * 采用构造器注入 StringRedisTemplate，确保依赖不可变且方便单元测试。
 * 该工具类通常由 {@link com.example.common.config.RedisAutoConfiguration} 自动配置为 Bean。
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
public class RedisUtil {
    private final StringRedisTemplate redisTemplate;

    /**
     * 构造器注入 StringRedisTemplate
     *
     * @param redisTemplate Spring Data Redis 提供的字符串模板，用于操作 Redis
     */
    public RedisUtil(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    /**
     * 设置字符串键值对，并指定过期时间
     *
     * @param key     键
     * @param value   值
     * @param timeout 过期时间数值
     * @param unit    时间单位（如 TimeUnit.SECONDS）
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }
    /**
     * 根据键获取字符串值
     *
     * @param key 键
     * @return 对应的值，若不存在则返回 null
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }
    /**
     * 删除指定的键
     *
     * @param key 键
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
