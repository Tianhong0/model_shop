package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "二手商品分页查询")
public class UsedListingQueryRequest {

    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小必须大于等于1")
    private Integer pageSize = 10;

    private String keyword;

    private Integer status;

    private Long sellerId;

    private String categoryName;

    private Boolean onlyMine;
}
