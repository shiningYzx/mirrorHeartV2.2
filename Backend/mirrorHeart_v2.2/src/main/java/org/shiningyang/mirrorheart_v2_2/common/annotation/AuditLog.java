package org.shiningyang.mirrorheart_v2_2.common.annotation;

import java.lang.annotation.*;

/**
 * 管理端审计日志注解
 * 用于标识需要记录操作日志的管理端接口
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    
    /**
     * 所属业务模块 (如: "帖子管理", "举报处理")
     */
    String module() default "";

    /**
     * 具体操作描述 (如: "删除帖子", "处理违规评论")
     */
    String operation() default "";
}