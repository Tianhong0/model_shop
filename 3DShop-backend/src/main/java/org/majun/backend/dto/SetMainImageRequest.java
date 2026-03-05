package org.majun.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设置主图请求DTO
 */
@Data
public class SetMainImageRequest {

    @NotNull(message = "模型ID不能为空")
    private Long modelId;

    @NotNull(message = "图片ID不能为空")
    private Long imageId;
}