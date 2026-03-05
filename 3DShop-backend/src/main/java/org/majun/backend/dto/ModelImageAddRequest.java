package org.majun.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加模型图片请求DTO
 */
@Data
public class ModelImageAddRequest {

    @NotNull(message = "模型ID不能为空")
    private Long modelId;

    @NotNull(message = "图片URL不能为空")
    private String imageUrl;

    private Integer isMain = 0;

    private Integer imgType = 1;

    private Integer sortOrder = 0;
}