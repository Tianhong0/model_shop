package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "After-sale message send request")
public class AfterSaleMessageSendRequest {

    @NotNull(message = "After-sale ID is required")
    private Long afterSaleId;

    @NotBlank(message = "Message content is required")
    private String content;

    @Schema(description = "Message type: 1-text, 2-image, 3-system")
    private Integer messageType = 1;

    @Schema(description = "Attachment URLs separated by comma")
    private String attachments;
}
