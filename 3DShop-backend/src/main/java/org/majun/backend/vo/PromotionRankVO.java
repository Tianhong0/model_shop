package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "推广排行榜")
public class PromotionRankVO {

    @Schema(description = "排名")
    private Integer rank;

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "邀请人数")
    private Integer inviteCount;

    @Schema(description = "获得积分")
    private Integer totalPoints;
}
