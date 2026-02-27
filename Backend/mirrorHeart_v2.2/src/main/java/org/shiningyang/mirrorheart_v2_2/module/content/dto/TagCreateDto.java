package org.shiningyang.mirrorheart_v2_2.module.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TagCreateDto {
        @NotBlank(message = "标签名不能为空")
        private String name;
        private String groupName;
    }