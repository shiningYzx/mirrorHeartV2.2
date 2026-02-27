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
 * 邮箱验证码
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("email_otp")
public class EmailOtp implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("email")
    private String email;

    /**
     * 业务场景: register/login/reset
     */
    @TableField("scene")
    private String scene;

    /**
     * 验证码字符
     */
    @TableField("code")
    private String code;

    /**
     * 过期时间
     */
    @TableField("expire_at")
    private LocalDateTime expireAt;

    /**
     * 使用时间，非空表示已验证
     */
    @TableField("used_at")
    private LocalDateTime usedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
