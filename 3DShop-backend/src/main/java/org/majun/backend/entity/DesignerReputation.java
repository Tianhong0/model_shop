package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设计者信誉实体
 */
@Data
@TableName("designer_reputation")
public class DesignerReputation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("designer_id")
    private Long designerId;

    @TableField("reputation_score")
    private Integer reputationScore;

    @TableField("total_tasks")
    private Integer totalTasks;

    @TableField("total_ratings")
    private Integer totalRatings;

    @TableField("avg_score")
    private BigDecimal avgScore;

    @TableField("five_star_count")
    private Integer fiveStarCount;

    @TableField("four_star_count")
    private Integer fourStarCount;

    @TableField("three_star_count")
    private Integer threeStarCount;

    @TableField("two_star_count")
    private Integer twoStarCount;

    @TableField("one_star_count")
    private Integer oneStarCount;

    @TableField("quality_answer_count")
    private Integer qualityAnswerCount;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
