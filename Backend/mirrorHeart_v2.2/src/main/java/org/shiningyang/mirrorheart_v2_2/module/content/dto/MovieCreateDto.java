package org.shiningyang.mirrorheart_v2_2.module.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class MovieCreateDto {
    
    @NotBlank(message = "电影名称不能为空")
    private String title;
    
    private String summary;
    private String coverUrl; // 海报
    private List<Long> tagIds;

    // --- Movie 子表字段 ---
    private String director; // 导演
    private String year; // 上映年份
}