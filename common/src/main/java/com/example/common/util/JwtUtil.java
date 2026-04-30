package com.example.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
/**
 * JWT（JSON Web Token）工具类
 * <p>
 * 提供生成 Token、解析 Token、验证 Token 等功能。
 * 采用 HS256 签名算法，Token 中包含用户名（subject）、签发时间、过期时间。
 * </p>
 * <p>
 * 注意：生产环境应将 secret 和 expiration 配置在配置中心（如 Nacos），
 * 避免硬编码在代码中。
 * </p>
 *
 * @author 胡孟阳
 * @since 2026-04-20
 */
public class JwtUtil {

    /**
     * 签名密钥（至少 32 字符）
     * TODO: 生产环境请从配置文件或配置中心读取
     */
    private static String secret = "yourSecretKey12345678901234567890"; // 至少32字符

    /**
     * Token 过期时间（毫秒），默认 1 小时
     * TODO: 生产环境请从配置文件或配置中心读取
     */
    private static Long expiration = 3600000L; // 1小时

    /**
     * 生成 JWT Token
     *
     * @param username 用户名（作为 Token 的主体）
     * @return 生成的 Token 字符串
     */
    public static String generateToken(Long userId,String username) {
        return Jwts.builder()
                .claim("userId",userId)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .compact();
    }

    /**
     * 从 Token 中解析出用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public static String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * 从 Token 中获取用户ID
     */
    public static Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secret.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", Long.class);
    }
    /**
     * 验证 Token 是否有效（未过期且签名正确）
     *
     * @param token JWT Token
     * @return true 有效，false 无效（过期或签名错误）
     */
    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secret.getBytes()).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}