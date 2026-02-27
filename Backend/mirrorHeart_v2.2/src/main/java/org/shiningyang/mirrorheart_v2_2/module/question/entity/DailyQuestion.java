package org.shiningyang.mirrorheart_v2_2.module.question.entity;

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
 * 问题库(不绑定日期)
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("daily_question")
public class DailyQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 问题文本
     */
    @TableField("text")
    private String text;

    /**
     * 话题/分类
     */
    @TableField("topic")
    private String topic;

    /**
     * 被推送次数
     */
    @TableField("use_count")
    private Integer useCount;

    /**
     * 1=启用, 0=停用
     */
    @TableField("status")
    private Byte status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
