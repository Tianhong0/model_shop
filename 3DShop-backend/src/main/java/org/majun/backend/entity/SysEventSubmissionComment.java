package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作品评论表
 */
@Data
@TableName("sys_event_submission_comment")
public class SysEventSubmissionComment {

    private Long id;

    private Long submissionId;

    private Long userId;

    private Long parentId;

    private Long replyToUserId;

    private String content;

    private Integer likeCount;

    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
