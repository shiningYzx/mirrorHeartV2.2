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
 * 消息通知表
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 接收者ID
     */
    @TableField("receiver_id")
    private Long receiverId;

    /**
     * 发送者ID(系统通知则为空)
     */
    @TableField("sender_id")
    private Long senderId;

    /**
     * 类型: LIKE/COMMENT/FOLLOW/SYSTEM
     */
    @TableField("type")
    private String type;

    /**
     * 关联对象类型: POST/COMMENT/DAILY_ANSWER
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 关联对象ID
     */
    @TableField("target_id")
    private Long targetId;

    /**
     * 通知简述/摘要
     */
    @TableField("content")
    private String content;

    /**
     * 0=未读, 1=已读
     */
    @TableField("is_read")
    private Byte isRead;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
