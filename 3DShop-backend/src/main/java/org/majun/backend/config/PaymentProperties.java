package org.majun.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 支付宝支付配置属性
 */
@Data
@ConfigurationProperties(prefix = "payment.alipay")
public class PaymentProperties {

    /** 应用ID */
    private String appId;

    /** 应用私钥 */
    private String privateKey;

    /** 应用公钥证书路径 */
    private String appCertPath;

    /** 支付宝公钥证书路径 */
    private String alipayPublicCertPath;

    /** 支付宝根证书路径 */
    private String alipayRootCertPath;

    /** 支付宝网关地址 */
    private String gatewayUrl;

    /** 异步通知地址 */
    private String notifyUrl;

    /** 同步跳转地址 */
    private String returnUrl;

    /**
     * 商户ID（PID）
     * 如果设置，回调时卖家ID必须匹配
     */
    private String sellerId;

    /** 签名类型，默认 RSA2 */
    private String signType = "RSA2";

    /** 字符集，默认 UTF-8 */
    private String charset = "UTF-8";

    /** 数据格式，默认 json */
    private String format = "json";

    /** 支付超时时间（分钟） */
    private Integer timeoutMinutes = 30;
}
