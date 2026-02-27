package org.shiningyang.mirrorheart_v2_2.module.recommend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 推荐内容详情
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("daily_recommendation_item")
public class DailyRecommendationItem {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long dailyId;

    private Long contentId;

    /**
     * 排序权重
     * 注意：rank 是 MySQL 8.0 保留关键字，必须使用 @TableField("`rank`") 进行转义
     */
    @TableField("`rank`")
    private Integer rank;
}
