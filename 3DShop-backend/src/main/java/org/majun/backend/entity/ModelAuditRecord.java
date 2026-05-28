package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模型审核记录实体类
 */
@Data
@TableName("model_audit_record")
@Schema(description = "模型审核记录")
public class ModelAuditRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("model_id")
    @Schema(description = "关联模型ID")
    private Long modelId;

    @TableField("audit_by")
    @Schema(description = "审核人ID")
    private Long auditBy;

    @TableField("action")
    @Schema(description = "审核动作: 1-通过, 2-驳回")
    private Integer action;

    @TableField("profit_share_ratio")
    @Schema(description = "通过时设置的分润比例")
    private Integer profitShareRatio;

    @TableField("note")
    @Schema(description = "审核备注/驳回原因")
    private String note;

    @TableField("snapshot_data")
    @Schema(description = "审核时的模型参数快照(JSON)")
    private String snapshotData;

    @TableField("is_delete")
    @Schema(description = "逻辑删除")
    private Integer isDelete;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private String createTime;
}
