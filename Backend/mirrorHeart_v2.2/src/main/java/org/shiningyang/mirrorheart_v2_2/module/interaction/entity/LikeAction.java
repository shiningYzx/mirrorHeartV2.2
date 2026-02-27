package org.shiningyang.mirrorheart_v2_2.module.interaction.entity;

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
 * 通用点赞表
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("like_action")
public class LikeAction implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /**
     * 类型: POST/COMMENT/CONTENT/DAILY_ANSWER
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 目标表的主键ID
     */
    @TableField("target_id")
    private Long targetId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
