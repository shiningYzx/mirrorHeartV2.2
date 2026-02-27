package org.shiningyang.mirrorheart_v2_2.module.community.dto;

import lombok.Data;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserInfoVo;

import java.time.LocalDateTime;

@Data
public class CommentVo {
    private Long id;
    private Long postId;
    private String text;

    // 新增：父评论ID和根评论ID
    private Long parentId;
    private Long rootId;

    // 作者信息
    private UserInfoVo author;
    // 新增：被回复人信息 (用于二级评论显示 "回复 @xxx")
    private UserInfoVo replyToUser;
    
    // 点赞数
    private Integer likeCount;
    
    // 发布时间
    private LocalDateTime createdAt;
    
    // 子评论数 (仅在一级评论中有效，用于前端显示 "展开x条回复")
    private Long childCount;

    private Boolean isLiked = false;
}