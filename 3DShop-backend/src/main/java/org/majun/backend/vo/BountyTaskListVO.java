package org.majun.backend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BountyTaskListVO {

    private Long id;
    private String taskSn;
    private String title;
    private Long publisherId;
    private BigDecimal budgetAmount;
    private BigDecimal finalAmount;
    private Integer status;
    private Integer bidCount;
    private LocalDateTime deadlineTime;
    private LocalDateTime createTime;
}
