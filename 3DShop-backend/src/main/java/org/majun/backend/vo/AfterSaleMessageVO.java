package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "After-sale message")
public class AfterSaleMessageVO {

    private Long id;
    private Long afterSaleId;
    private Long senderId;
    private String senderRole;
    private Integer messageType;
    private String content;
    private String attachments;
    private Integer isSystem;
    private LocalDateTime createTime;
}
