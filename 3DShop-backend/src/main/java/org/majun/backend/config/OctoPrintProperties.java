package org.majun.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OctoPrint 3D打印机配置属性
 * 用于连接和控制3D打印机
 */
@Data
@Component
@ConfigurationProperties(prefix = "octoprint")
public class OctoPrintProperties {

    private String authHeaderKey = "X-Api-Key";

    private String authHeaderValue;

    private Integer connectTimeoutMs = 5000;

    private Integer readTimeoutMs = 15000;

    private Integer pollIntervalMs = 5000;
}
