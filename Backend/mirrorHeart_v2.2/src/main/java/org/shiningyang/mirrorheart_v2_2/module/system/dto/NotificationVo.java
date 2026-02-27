package org.shiningyang.mirrorheart_v2_2.module.system.dto;

import lombok.Data;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserInfoVo;

import java.time.LocalDateTime;

@Data
public class NotificationVo {
    private Long id;
    
    // 发送者信息 (如点赞的人)
    private UserInfoVo sender;

    // 类型: LIKE, COMMENT, FOLLOW, SYSTEM
    private String type;

    // 关联目标类型: POST, COMMENT, DAILY_ANSWER
    private String targetType;
    private Long targetId;

    // 通知内容描述 (如帖子正文片段或自定义消息)
    private String content;

    private Integer isRead; // 0=未读, 1=已读
    private LocalDateTime createdAt;
}