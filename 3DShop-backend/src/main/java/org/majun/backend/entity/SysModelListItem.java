package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_model_list_item")
@Schema(description = "模型清单项")
public class SysModelListItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("list_id")
    @Schema(description = "清单ID")
    private Long listId;

    @TableField("model_id")
    @Schema(description = "模型ID")
    private Long modelId;

    @TableField("sort_no")
    @Schema(description = "排序号")
    private Integer sortNo;

    @TableField("remark")
    @Schema(description = "推荐理由")
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
