package org.shiningyang.mirrorheart_v2_2.module.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class QuoteCreateDto {
    
    // --- Content 主表字段 ---
    @NotBlank(message = "标题不能为空") // 金句的标题可以是第一句，或者空
    private String title;
    
    private String summary;
    
    private String coverUrl; // 金句背景图
    
    // 标签ID列表，用于关联 content_tag_relation
    private List<Long> tagIds;

    // --- Quote 子表字段 ---
    @NotBlank(message = "金句内容不能为空")
    private String text;
    
    @NotBlank(message = "作者不能为空")
    private String author;
    
    private String source; // 出处 (书名/电影名)
}