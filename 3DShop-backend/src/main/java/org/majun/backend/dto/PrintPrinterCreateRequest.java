package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "新增打印机请求")
public class PrintPrinterCreateRequest {

    @NotBlank(message = "打印机名称不能为空")
    @Schema(description = "打印机名称")
    private String printerName;

    @NotBlank(message = "打印机IP不能为空")
    @Schema(description = "OctoPrint服务器IP")
    private String ip;

    @Min(value = 1, message = "端口范围错误")
    @Max(value = 65535, message = "端口范围错误")
    @Schema(description = "OctoPrint端口", example = "5000")
    private Integer port = 5000;

    @Schema(description = "是否HTTPS")
    private Boolean https = false;

    @Schema(description = "认证Header Key，默认 X-Api-Key")
    private String authHeaderKey = "X-Api-Key";

    @Schema(description = "该打印机API Key，可不填(走全局配置)")
    private String authHeaderValue;

    @Schema(description = "排序")
    private Integer sort = 0;
}
