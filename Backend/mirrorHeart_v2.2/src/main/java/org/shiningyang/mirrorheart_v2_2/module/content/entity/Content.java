package org.shiningyang.mirrorheart_v2_2.module.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 内容主表
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("content")
public class Content implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 类型枚举: QUOTE/ARTICLE/BOOK/PAINTING/MUSIC/MOVIE
     */
    @TableField("type")
    private String type;

    /**
     * 标题
     */
    @TableField("title")
    private String title;

    /**
     * 摘要/简介
     */
    @TableField("summary")
    private String summary;

    /**
     * 封面图
     */
    @TableField("cover_url")
    private String coverUrl;

    /**
     * 来源/出处
     */
    @TableField("source")
    private String source;

    /**
     * 状态: 1=已发布, 0=草稿
     */
    @TableField("status")
    private Byte status;

    /**
     * 点赞数冗余，用于快速排序
     */
    @TableField("like_count")
    private Integer likeCount;

    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
