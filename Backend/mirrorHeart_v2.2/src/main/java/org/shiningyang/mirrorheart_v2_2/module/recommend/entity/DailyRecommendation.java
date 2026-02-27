package org.shiningyang.mirrorheart_v2_2.module.recommend.entity;

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
 * 每日推荐记录
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("daily_recommendation")
public class DailyRecommendation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /**
     * 推荐日期
     */
    @TableField("day")
    private LocalDate day;

    /**
     * 推荐策略/算法版本
     */
    @TableField("strategy")
    private String strategy;

    /**
     * 用户查看时间，为空表示未读
     */
    @TableField("viewed_at")
    private LocalDateTime viewedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
