package org.majun.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 密钥
     */
    private String secret = "3DShop-backend-secret-key-for-jwt-token-generation-please-change-in-production";

    /**
     * Token过期时间（毫秒），默认7天
     */
    private Long expiration = 7 * 24 * 60 * 60 * 1000L;

    /**
     * Token头部前缀
     */
    private String header = "Authorization";

    /**
     * Token前缀
     */
    private String prefix = "Bearer ";
}
