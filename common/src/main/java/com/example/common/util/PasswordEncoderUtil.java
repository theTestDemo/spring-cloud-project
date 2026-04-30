package com.example.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
/**
 * 密码加密工具类
 * <p>
 * 基于 BCrypt 算法进行密码加密和校验。
 * BCrypt 是一种单向哈希算法，内置盐值，安全性高，适合用户密码存储。
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
public class PasswordEncoderUtil {
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    /**
     * 加密明文密码
     *
     * @param rawPassword 明文密码
     * @return 加密后的密文（BCrypt 格式，包含盐值和哈希）
     */
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }
    /**
     * 校验明文密码与密文是否匹配
     *
     * @param rawPassword     明文密码
     * @param encodedPassword 数据库中存储的 BCrypt 密文
     * @return true 匹配成功，false 匹配失败
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return ENCODER.matches(rawPassword, encodedPassword);
    }
}