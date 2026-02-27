package org.shiningyang.mirrorheart_v2_2.module.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 每日一问记录(推送+回答)
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("user_daily_record")
public class UserDailyRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /**
     * 日期
     */
    @TableField("day")
    private LocalDate day;

    /**
     * 当日推送的问题ID
     */
    @TableField("question_id")
    private Long questionId;

    /**
     * 文本回答内容
     */
    private String answerText;

    /**
     * 回答语音
     */
    @TableField("audio_url")
    private String audioUrl;

    @TableField("duration_ms")
    private Integer durationMs;

    /**
     * 回答时间
     */
    @TableField("answered_at")
    private LocalDateTime answeredAt;

    /**
     * 0=公开, 1=粉丝可见, 2=私密
     */
    @TableField("visibility")
    private Byte visibility;

    /**
     * 点赞数
     */
    @TableField("like_count")
    private Integer likeCount;

    /**
     * 热度值(用于热门回答排序)
     */
    @TableField("heat_score")
    private Long heatScore;

    /**
     * 推送时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
