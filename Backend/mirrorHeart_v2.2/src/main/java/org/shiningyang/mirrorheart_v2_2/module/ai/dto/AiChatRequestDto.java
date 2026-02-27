package org.shiningyang.mirrorheart_v2_2.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatRequestDto {
    // 会话ID。如果不传，则表示开启新会话
    private Long sessionId;

    @NotBlank(message = "发送内容不能为空")
    private String content;
    
    // 是否强制开启深度思考 (enabled/disabled/auto)，默认 auto
    private String thinking = "auto";
}