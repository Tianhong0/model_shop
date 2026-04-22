package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "被邀请人信息")
public class InviteeVO {

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "注册时间")
    private LocalDateTime registerTime;

    @Schema(description = "首单时间")
    private LocalDateTime firstOrderTime;

    @Schema(description = "订单数")
    private Integer orderCount;

    @Schema(description = "订单金额")
    private BigDecimal orderAmount;

    @Schema(description = "贡献积分")
    private Integer contributedPoints;
}
