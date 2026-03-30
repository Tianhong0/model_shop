package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作品点赞表
 */
@Data
@TableName("sys_event_submission_like")
public class SysEventSubmissionLike {

    private Long id;

    private Long submissionId;

    private Long userId;

    private LocalDateTime createTime;
}
