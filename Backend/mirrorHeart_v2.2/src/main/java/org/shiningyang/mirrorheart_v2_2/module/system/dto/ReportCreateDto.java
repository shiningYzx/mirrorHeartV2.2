package org.shiningyang.mirrorheart_v2_2.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReportCreateDto {
    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    @NotBlank(message = "举报原因不能为空")
    @Size(max = 200, message = "举报原因最多200字")
    private String reason;
}