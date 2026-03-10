package org.majun.backend.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UsedOrderQueryRequest {

    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小必须大于等于1")
    private Integer pageSize = 10;

    private Integer status;

    private Long sellerId;

    private Long buyerId;

    private String keyword;
}
