package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 用户优惠券查询请求
 */
@Data
@Schema(description = "用户优惠券查询请求")
public class UserCouponQueryRequest {

    @Min(value = 1, message = "页码必须大于等于1")
    @Schema(description = "页码")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页大小必须大于等于1")
    @Schema(description = "每页大小")
    private Integer pageSize = 10;

    @Schema(description = "状态：0-未使用，1-已使用，2-已过期，不传查全部")
    private Integer status;
}
