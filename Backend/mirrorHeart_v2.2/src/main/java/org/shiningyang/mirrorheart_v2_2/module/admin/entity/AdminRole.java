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
 * RBAC角色表
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("admin_role")
public class AdminRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 角色显示名: 如 内容管理员
     */
    @TableField("role_name")
    private String roleName;

    /**
     * 程序用唯一标识: 如 content_admin
     */
    @TableField("role_key")
    private String roleKey;

    /**
     * 角色职责描述
     */
    @TableField("description")
    private String description;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
