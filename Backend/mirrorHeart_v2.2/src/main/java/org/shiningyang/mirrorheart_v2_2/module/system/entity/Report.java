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
 * 举报/反馈表
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("report")
public class Report implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 举报人ID
     */
    @TableField("reporter_id")
    private Long reporterId;

    /**
     * 目标类型: POST/COMMENT/USER
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 目标ID
     */
    @TableField("target_id")
    private Long targetId;

    /**
     * 举报理由
     */
    @TableField("reason")
    private String reason;

    /**
     * 0=待处理, 1=已处理, 2=忽略
     */
    @TableField("status")
    private Byte status;

    /**
     * 管理员处理批注
     */
    @TableField("admin_note")
    private String adminNote;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
