package org.majun.backend.config;

import io.minio.MinioClient;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

/**
 * MinIO 对象存储配置类
 * 配置文件存储服务的客户端连接
 */
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint; // 后端内部使用：127.0.0.1:9000

    @Value("${minio.public-endpoint}")
    private String publicEndpoint; // 前端访问：公网 IP:9000

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secretKey}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();

        return MinioClient.builder()
                .endpoint(endpoint) // 后端内部用 internal endpoint
                .httpClient(httpClient)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 获取后端内部使用的 endpoint（127.0.0.1）
     */
    public String getInternalEndpoint() {
        return endpoint;
    }

    /**
     * 获取前端公开访问的 endpoint（公网 IP）
     */
    public String getPublicEndpoint() {
        return publicEndpoint;
    }
}