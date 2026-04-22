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
@Schema(description = "邀请码信息")
public class InviteCodeVO {

    @Schema(description = "邀请码")
    private String inviteCode;

    @Schema(description = "累计邀请人数")
    private Integer totalInvited;

    @Schema(description = "累计获得积分")
    private Integer totalPointsEarned;

    @Schema(description = "邀请链接")
    private String inviteLink;

    @Schema(description = "邀请海报URL")
    private String posterUrl;
}
