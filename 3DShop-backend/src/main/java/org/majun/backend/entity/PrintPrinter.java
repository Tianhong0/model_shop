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

@Data
@TableName("print_printer")
@Schema(description = "打印机")
public class PrintPrinter implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("printer_code")
    private String printerCode;

    @TableField("printer_name")
    private String printerName;

    @TableField("base_url")
    private String baseUrl;

    @TableField("auth_header_key")
    private String authHeaderKey;

    @TableField("auth_header_value")
    private String authHeaderValue;

    @TableField("status")
    private Integer status;

    @TableField("current_job_id")
    private Long currentJobId;

    @TableField("sort")
    private Integer sort;

    @TableLogic
    @TableField("is_delete")
    private Integer isDelete;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
