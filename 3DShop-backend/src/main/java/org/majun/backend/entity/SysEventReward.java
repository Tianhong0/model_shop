package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动奖励实体
 */
@Data
@TableName("sys_event_reward")
@Schema(description = "活动奖励")
public class SysEventReward implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "奖励ID")
    private Long id;

    @TableField("event_id")
    @Schema(description = "活动ID")
    private Long eventId;

    @TableField("rank_name")
    @Schema(description = "奖项名称")
    private String rankName;

    @TableField("rank_order")
    @Schema(description = "奖项排序")
    private Integer rankOrder;

    @TableField("winner_count")
    @Schema(description = "获奖人数")
    private Integer winnerCount;

    @TableField("prize_content")
    @Schema(description = "奖品内容")
    private String prizeContent;

    @TableField("is_delete")
    @TableLogic
    @Schema(description = "逻辑删除")
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
