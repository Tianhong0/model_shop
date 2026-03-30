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
import java.time.LocalDateTime;

/**
 * 评价申诉实体
 */
@Data
@TableName("bounty_rating_appeal")
public class BountyRatingAppeal implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("rating_id")
    private Long ratingId;

    @TableField("designer_id")
    private Long designerId;

    @TableField("reason")
    private String reason;

    @TableField("evidence")
    private String evidence;

    @TableField("status")
    private Integer status;

    @TableField("admin_id")
    private Long adminId;

    @TableField("admin_remark")
    private String adminRemark;

    @TableField("processed_time")
    private LocalDateTime processedTime;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
