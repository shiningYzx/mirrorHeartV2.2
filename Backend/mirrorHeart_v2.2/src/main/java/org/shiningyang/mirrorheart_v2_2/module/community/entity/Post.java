package org.shiningyang.mirrorheart_v2_2.module.community.entity;

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
 * 帖子表
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("post")
public class Post implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 发帖人
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 可选标题
     */
    @TableField("title")
    private String title;

    /**
     * 帖子正文
     */
    @TableField("text")
    private String text;

    /**
     * 0=公开, 1=粉丝可见, 2=私密
     */
    @TableField("visibility")
    private Byte visibility;

    /**
     * 1=正常, 0=审核中, 2=封禁
     */
    @TableField("status")
    private Byte status;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("comment_count")
    private Integer commentCount;

    @TableField("favorite_count")
    private Integer favoriteCount;

    @TableField("view_count")
    private Integer viewCount;

    /**
     * 算法计算的热度值
     */
    @TableField("heat_score")
    private Long heatScore;

    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Boolean isLiked = false; // 🌟 当前登录用户是否已点赞

    @TableField(exist = false)
    private Boolean isFavorited = false; // 🌟 顺便把是否已收藏也加上，方便前端使用
}
