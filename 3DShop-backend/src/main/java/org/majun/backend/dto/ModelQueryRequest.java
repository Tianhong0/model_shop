package org.majun.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 模型查询请求DTO
 */
@Data
@Schema(description = "模型查询请求")
public class ModelQueryRequest {

    /**
     * 页码
     */
    @Min(value = 1, message = "页码最小值为1")
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小最小值为1")
    private Integer pageSize = 10;

    /**
     * 分类ID
     */
    @Schema(description = "分类ID")
    private Long categoryId;

    /**
     * 模型名称（模糊搜索）
     */
    @Schema(description = "模型名称")
    private String modelName;

    /**
     * 设计者ID
     */
    @Schema(description = "设计者ID")
    private Long designerId;

    /**
     * 上架状态: 0-审核中, 1-上架, 2-下架
     */
    @Schema(description = "上架状态: 0-审核中, 1-上架, 2-下架")
    private Integer status;

    /**
     * 来源类型: 1-OFFICIAL(官方), 2-DESIGNER(设计者作品)
     */
    @Schema(description = "来源类型: 1-官方, 2-设计者作品")
    private Integer sourceType;

    /**
     * 排序字段: create_time-创建时间降序, create_time_asc-创建时间升序,
     *          price_asc-价格升序, price_desc-价格降序,
     *          sales-销量降序, sales_asc-销量升序,
     *          score-评分降序, score_asc-评分升序
     */
    @Schema(description = "排序字段")
    private String orderBy = "create_time";
}
