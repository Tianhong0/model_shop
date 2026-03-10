package org.majun.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UsedListingUpdateRequest extends UsedListingCreateRequest {

    @NotNull(message = "商品ID不能为空")
    private Long id;
}
