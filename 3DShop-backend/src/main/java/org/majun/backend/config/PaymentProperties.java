package org.majun.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Payment configuration properties.
 */
@Data
@ConfigurationProperties(prefix = "payment.alipay")
public class PaymentProperties {

    private String appId;

    private String privateKey;

    private String appCertPath;

    private String alipayPublicCertPath;

    private String alipayRootCertPath;

    private String gatewayUrl;

    private String notifyUrl;

    private String returnUrl;

    /**
     * Optional seller id (PID). If set, callback seller_id must match.
     */
    private String sellerId;

    private String signType = "RSA2";

    private String charset = "UTF-8";

    private String format = "json";

    private Integer timeoutMinutes = 30;
}
