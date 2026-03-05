package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Delivery track")
public class DeliveryTrackVO {

    @Schema(description = "Track ID")
    private Long id;

    @Schema(description = "Track content")
    private String trackContent;

    @Schema(description = "Track time")
    private LocalDateTime trackTime;

    @Schema(description = "Operator info")
    private String operatorInfo;
}
