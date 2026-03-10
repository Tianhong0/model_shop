package org.majun.backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UsedReportVO {

    private Long id;
    private Long reporterId;
    private String reporterNickname;
    private String targetType;
    private Long targetId;
    private String reasonType;
    private String reasonText;
    private String evidenceUrls;
    private Integer status;
    private String handleAction;
    private String handleRemark;
    private Long handlerId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
