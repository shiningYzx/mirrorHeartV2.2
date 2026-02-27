package org.shiningyang.mirrorheart_v2_2.module.question.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuestionCreateDto {
    @NotBlank(message = "题目内容不能为空")
    private String text;

    private String topic; // 话题标签，如"情感","职场"
}