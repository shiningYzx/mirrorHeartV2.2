package org.shiningyang.mirrorheart_v2_2.module.question.dto;

import lombok.Data;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserInfoVo;

import java.time.LocalDateTime;

/**
 * 每日一问 - 用户回答视图对象
 */
@Data
public class UserDailyRecordVo {
    
    /**
     * 回答记录ID
     */
    private Long id;
    
    /**
     * 回答者ID
     */
    private Long userId;
    
    /**
     * 关联的每日一问ID
     */
    private Long questionId;
    
    /**
     * 回答内容 (以你实际数据库中的字段名为准，可能是 content 或 answer)
     */
    private String content;

    /**
     * 语音回答链接 (如果用户使用了语音)
     */
    private String audioUrl;

    /**
     * 音频时长(毫秒)
     */
    private Integer durationMs;

    /**
     * 点赞数
     */
    private Integer likeCount;

    
    /**
     * 可见性：0=公开, 1=仅自己可见, 2=仅粉丝可见等 (以你的业务设定为准)
     */
    private Byte visibility;
    
    /**
     * 回答时间
     */
    private LocalDateTime createdAt;

    // ==========================================
    // 🌟 以下为动态装配字段 (数据库中不存在，组装后传给前端)
    // ==========================================

    /**
     * 当前登录用户是否已点赞
     */
    private Boolean isLiked = false;

    /**
     * 回答者的详细信息 (头像、昵称等)
     */
    private UserInfoVo author;
}