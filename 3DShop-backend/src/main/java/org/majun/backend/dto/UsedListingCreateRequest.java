package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "创建二手商品")
public class UsedListingCreateRequest {

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "描述不能为空")
    private String description;

    @NotBlank(message = "封面不能为空")
    private String coverUrl;

    @NotEmpty(message = "至少上传一张图片")
    private List<String> imageUrls;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.01", message = "售价必须大于0")
    private BigDecimal price;

    private BigDecimal originalPrice;

    @NotBlank(message = "成色不能为空")
    private String conditionLevel;

    private String categoryName;

    private String location;
}
