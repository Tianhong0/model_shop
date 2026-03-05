package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "打印机")
public class PrintPrinterVO {
    private Long id;
    private String printerCode;
    private String printerName;
    private String baseUrl;
    private String authHeaderKey;
    private Integer status;
    private String statusDesc;
    private Long currentJobId;
    private Integer sort;
}
