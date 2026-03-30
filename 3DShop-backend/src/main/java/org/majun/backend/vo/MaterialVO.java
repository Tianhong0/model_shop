package org.majun.backend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 材质VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "材质")
public class MaterialVO {

    /**
     * 材质ID
     */
    @Schema(description = "材质ID")
    private Long id;

    /**
     * 材质名称
     */
    @Schema(description = "材质名称")
    private String name;

    /**
     * 单价 (元/克)
     */
    @Schema(description = "单价(元/克)")
    private BigDecimal price;

    /**
     * 是否信任材质
     */
    @Schema(description = "是否信任材质")
    private Boolean isTrusted;

    /**
     * 是否环保材质
     */
    @Schema(description = "是否环保材质")
    private Boolean isEco;
}
