package org.majun.backend.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * 分类VO（支持树形结构）
 */
@Data
@Builder
public class CategoryVO {
    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 分类图标
     */
    private String icon;

    /**
     * 父级分类ID
     */
    private Long parentId;

    /**
     * 排序编号
     */
    private Integer sortNo;

    /**
     * 状态：1-启用, 0-禁用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 子分类列表
     */
    private List<CategoryVO> children;
}
