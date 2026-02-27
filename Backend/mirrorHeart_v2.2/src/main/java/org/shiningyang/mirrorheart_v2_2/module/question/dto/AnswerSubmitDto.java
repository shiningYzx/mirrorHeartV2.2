package org.shiningyang.mirrorheart_v2_2.module.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnswerSubmitDto {
    @NotNull(message = "问题ID不能为空")
    private Long questionId;

    private String audioUrl;

    private Integer durationMs; // 音频时长(毫秒)

    private String text; // 新增：文本回答
    
    // 可见性：0=公开, 1=粉丝可见, 2=私密 (默认为0)
    private Integer visibility = 0; 
}