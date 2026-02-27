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
 * 评论表
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Getter
@Setter
@TableName("comment")
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("post_id")
    private Long postId;

    /**
     * 评论人
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 评论内容
     */
    @TableField("text")
    private String text;

    /**
     * 被回复的父评论ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 所属的根评论ID
     */
    @TableField("root_id")
    private Long rootId;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
