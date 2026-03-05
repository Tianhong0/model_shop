package org.majun.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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
