package org.shiningyang.mirrorheart_v2_2.module.auth.entity;

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
 * 认证会话表
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("auth_session")
public class AuthSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /**
     * JWT ID (UUID)，对应 Token 中的 jti 字段
     */
    @TableField("jti")
    private String jti;

    /**
     * 设备名称
     */
    @TableField("device")
    private String device;

    /**
     * 登录IP
     */
    @TableField("ip")
    private String ip;

    /**
     * Token 自然过期时间
     */
    @TableField("expired_at")
    private LocalDateTime expiredAt;

    /**
     * 非空则表示被手动吊销/踢出
     */
    @TableField("revoked_at")
    private LocalDateTime revokedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
