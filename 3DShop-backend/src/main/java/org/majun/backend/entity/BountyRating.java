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
 * 悬赏评价实体
 */
@Data
@TableName("bounty_rating")
public class BountyRating implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("publisher_id")
    private Long publisherId;

    @TableField("designer_id")
    private Long designerId;

    @TableField("delivery_id")
    private Long deliveryId;

    @TableField("score")
    private Integer score;

    @TableField("comment")
    private String comment;

    @TableField("images")
    private String images;

    @TableField("is_anonymous")
    private Integer isAnonymous;

    @TableField("status")
    private Integer status;

    @TableField("admin_remark")
    private String adminRemark;

    @TableField("is_delete")
    @TableLogic
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
