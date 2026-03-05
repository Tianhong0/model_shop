package org.majun.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新图片排序请求DTO
 */
@Data
public class UpdateImageSortRequest {

    @NotNull(message = "图片ID不能为空")
    private Long imageId;

    @NotNull(message = "排序值不能为空")
    private Integer sortOrder;
}