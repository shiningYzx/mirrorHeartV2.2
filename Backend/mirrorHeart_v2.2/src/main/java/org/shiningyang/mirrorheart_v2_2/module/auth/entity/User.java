package org.shiningyang.mirrorheart_v2_2.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 用户基础表
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 登录邮箱，作为唯一凭证
     */
    @TableField("email")
    private String email;

    /**
     * Bcrypt算法加密后的密码
     */
    @TableField("password_hash")
    private String passwordHash;

    /**
     * 用户昵称
     */
    @TableField("nickname")
    private String nickname;

    /**
     * 头像URL
     */
    @TableField("avatar_url")
    private String avatarUrl;

    /**
     * 个人简介/签名
     */
    @TableField("bio")
    private String bio;

    @TableField("role")
    private Byte role;

    @TableField("status")
    private Byte status;

    /**
     * 逻辑删除标记
     */
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;

    /**
     * 最后登录时间
     */
    @TableField("last_login_at")
    private LocalDateTime lastLoginAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    // 🌟 新增的三个隐私字段
    @TableField("show_following")
    private Byte showFollowing;
    @TableField("show_favorite")
    private Byte showFavorite;
    @TableField("show_post")
    private Byte showPost;

    // 🌟 新增：生日相关字段
    @TableField("birthday")
    private LocalDate birthday;
    @TableField("birthday_update_time")
    private LocalDateTime birthdayUpdateTime;
}
