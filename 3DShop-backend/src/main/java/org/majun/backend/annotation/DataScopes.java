package org.majun.backend.annotation;

import java.lang.annotation.*;

/**
 * 多数据权限注解
 * 用于多表关联查询时的数据权限控制
 *
 * <p>使用示例：
 * <pre>
 * {@literal @}DataScopes(value = {
 *     {@literal @}DataScope(resourceType = "ORDER", ownerField = "user_id", tableAlias = "o"),
 *     {@literal @}DataScope(resourceType = "MODEL", ownerField = "designer_id", tableAlias = "m")
 * })
 * List&lt;OrderVO&gt; selectOrdersWithModel();
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScopes {

    /**
     * 数据权限配置数组
     */
    DataScope[] value();
}
