package org.shiningyang.mirrorheart_v2_2.module.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class PaintingCreateDto {
    
    @NotBlank(message = "画作名称不能为空")
    private String title;
    
    private String summary; // 赏析/简介
    
    @NotBlank(message = "画作图片不能为空")
    private String coverUrl;
    
    private List<Long> tagIds;

    // --- Painting 子表字段 ---
    @NotBlank(message = "画家不能为空")
    private String painter;
    
    private String year; // 创作年份
}