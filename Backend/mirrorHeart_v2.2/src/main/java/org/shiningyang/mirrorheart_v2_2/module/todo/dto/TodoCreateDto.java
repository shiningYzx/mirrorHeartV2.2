package org.shiningyang.mirrorheart_v2_2.module.todo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TodoCreateDto {
    @NotBlank(message = "待办事项标题不能为空")
    private String title;
}