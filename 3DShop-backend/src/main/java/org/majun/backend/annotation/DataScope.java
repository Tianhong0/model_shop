package org.majun.backend.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * 标注在Mapper接口方法或Service方法上，用于自动添加数据范围过滤条件
 *
 * <p>使用示例：
 * <pre>
 * // 单表查询
 * {@literal @}DataScope(resourceType = "ORDER", ownerField = "user_id")
 * List&lt;SysOrder&gt; selectUserOrders();
 *
 * // Service方法
 * {@literal @}DataScope(resourceType = "MODEL", ownerField = "designer_id")
 * public PageResult&lt;ModelVO&gt; getDesignerModels(Long designerId) { ... }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 资源类型
     * 如：ORDER、MODEL、BOUNTY、USED_LISTING、WALLET、POST等
     */
    String resourceType() default "";

    /**
     * 归属字段名
     * 如：user_id、designer_id、publisher_id、seller_id
     */
    String ownerField() default "user_id";

    /**
     * 表别名（多表查询时使用）
     */
    String tableAlias() default "";

    /**
     * 是否启用数据权限过滤
     * 某些场景可能需要临时禁用
     */
    boolean enabled() default true;

    /**
     * 管理员是否跳过数据权限检查
     * 默认跳过，即管理员拥有全部数据权限
     */
    boolean adminSkip() default true;
}
