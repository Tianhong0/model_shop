package org.majun.backend.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论点赞表
 */
@Data
@TableName("sys_event_comment_like")
public class SysEventCommentLike {

    private Long id;

    private Long commentId;

    private Long userId;

    private LocalDateTime createTime;
}
