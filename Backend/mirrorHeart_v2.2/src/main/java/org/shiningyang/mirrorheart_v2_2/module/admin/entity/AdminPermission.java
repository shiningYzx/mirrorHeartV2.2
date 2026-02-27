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
 * RBAC权限表
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("admin_permission")
public class AdminPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 父ID，用于构建菜单树
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 权限名: 如 删除帖子
     */
    @TableField("permission_name")
    private String permissionName;

    /**
     * 鉴权标识: 如 post:delete
     */
    @TableField("permission_key")
    private String permissionKey;

    /**
     * 1=菜单(Menu), 2=按钮/API(Button)
     */
    @TableField("type")
    private Byte type;

    /**
     * 前端路由或后端API路径
     */
    @TableField("path")
    private String path;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
