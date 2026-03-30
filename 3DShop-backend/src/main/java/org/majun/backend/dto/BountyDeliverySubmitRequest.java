package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "交付提交请求")
public class BountyDeliverySubmitRequest {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotBlank(message = "交付说明不能为空")
    private String description;

    @Schema(description = "是否最终交付:1是,0否")
    private Integer isFinal = 1;

    @NotEmpty(message = "交付文件不能为空，请上传模型文件")
    @Schema(description = "交付文件URL列表")
    private List<String> fileUrls;

    @Schema(description = "允许商业使用:0否,1是")
    private Integer allowCommercialUse = 0;

    @Schema(description = "允许修改:0否,1是")
    private Integer allowModification = 1;

    @Schema(description = "授权类型:Personal个人/Commercial商业/Custom自定义")
    private String licenseType = "Personal";
}
