package org.majun.backend.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标注在需要记录日志的方法上
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作类型
     */
    String type() default "OTHER";

    /**
     * 模块名称
     */
    String module() default "";

    /**
     * 操作描述
     */
    String description() default "";

    /**
     * 操作对象类型
     */
    String targetType() default "";

    /**
     * 是否记录请求参数
     */
    boolean recordParams() default true;

    /**
     * 是否记录返回结果
     */
    boolean recordResult() default false;

    /**
     * 是否记录变更前数据
     * 需要在方法中手动设置
     */
    boolean recordBeforeData() default false;
}
