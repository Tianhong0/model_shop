package org.majun.backend.config;

import io.minio.MinioClient;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        // 1. 创建兼容 JDK 21 的 OkHttpClient，强制使用 HTTP/1.1
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();

        // 2. 构建 MinioClient
        return MinioClient.builder()
                .endpoint(endpoint)
                .httpClient(httpClient)
                .credentials(accessKey, secretKey)
                .build();
    }
}