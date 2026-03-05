package org.majun.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.majun.backend.config.JwtProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Component
public class JwtUtil {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 生成密钥
     */
    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成Token
     *
     * @param userId   用户ID
     * @param userName 用户名
     * @return Token
     */
    public String generateToken(Long userId, String userName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userName", userName);
        return generateToken(claims);
    }

    /**
     * 生成Token
     *
     * @param claims 载荷数据
     * @return Token
     */
    public String generateToken(Map<String, Object> claims) {
        long now = System.currentTimeMillis();
        long expiration = now + jwtProperties.getExpiration();

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(now))
                .expiration(new Date(expiration))
                .signWith(getSignKey())
                .compact();
    }

    /**
     * 解析Token
     *
     * @param token Token
     * @return 载荷数据
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取用户ID
     *
     * @param token Token
     * @return 用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 获取用户名
     *
     * @param token Token
     * @return 用户名
     */
    public String getUserName(String token) {
        Claims claims = parseToken(token);
        return claims.get("userName", String.class);
    }

    /**
     * 验证Token是否有效
     *
     * @param token Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取Token过期时间（时间戳）
     *
     * @param token Token
     * @return 过期时间（时间戳）
     */
    public long getExpiration(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration().getTime();
    }
}
