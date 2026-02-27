package org.shiningyang.mirrorheart_v2_2.module.system.entity;

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
 * App版本管理
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("app_version")
public class AppVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 平台: ANDROID/IOS
     */
    @TableField("platform")
    private String platform;

    /**
     * 版本号(整数, 如 101)
     */
    @TableField("version_code")
    private Integer versionCode;

    /**
     * 版本名(如 1.0.1)
     */
    @TableField("version_name")
    private String versionName;

    /**
     * 是否强制更新
     */
    @TableField("is_force")
    private Byte isForce;

    /**
     * 下载/跳转地址
     */
    @TableField("download_url")
    private String downloadUrl;

    /**
     * 更新日志
     */
    @TableField("update_log")
    private String updateLog;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
