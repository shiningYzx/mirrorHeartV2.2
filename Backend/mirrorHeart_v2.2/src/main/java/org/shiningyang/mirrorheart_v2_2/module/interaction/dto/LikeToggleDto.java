package org.shiningyang.mirrorheart_v2_2.module.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LikeToggleDto {
    
    @NotBlank(message = "目标类型不能为空")
    // 可选值: POST, COMMENT, CONTENT, DAILY_ANSWER
    private String targetType;

    @NotNull(message = "目标ID不能为空")
    private Long targetId;
}