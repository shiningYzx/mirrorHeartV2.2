package org.shiningyang.mirrorheart_v2_2.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 管理端审计日志
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("admin_audit_log")
public class AdminAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作人ID(管理员)
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 业务模块: Post, User, System
     */
    @TableField("module")
    private String module;

    /**
     * 具体操作: Delete, Ban
     */
    @TableField("operation")
    private String operation;

    /**
     * 被操作对象的ID快照
     */
    @TableField("target_id")
    private String targetId;

    /**
     * 操作者IP
     */
    @TableField("ip")
    private String ip;

    /**
     * 请求参数快照(JSON)
     */
    @TableField("params")
    private String params;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
