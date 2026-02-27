package org.shiningyang.mirrorheart_v2_2.module.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post_view_history")
public class PostViewHistory {
    
    @TableId(type = IdType.AUTO)
    @TableField("id")
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("post_id")
    private Long postId;

    @TableField("viewed_at")
    private LocalDateTime viewedAt;
}