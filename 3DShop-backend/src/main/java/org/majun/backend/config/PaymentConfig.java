package org.majun.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付配置类
 * 启用支付相关配置属性
 */
@Configuration
@EnableConfigurationProperties({PaymentProperties.class})
public class PaymentConfig {
}
