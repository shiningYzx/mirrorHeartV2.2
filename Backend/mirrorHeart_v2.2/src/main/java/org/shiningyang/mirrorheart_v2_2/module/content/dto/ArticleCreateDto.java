package org.shiningyang.mirrorheart_v2_2.module.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ArticleCreateDto {
    
    // --- Content 主表字段 ---
    @NotBlank(message = "标题不能为空")
    private String title;
    
    private String summary;
    private String coverUrl;
    private String source; // 来源
    private List<Long> tagIds;

    // --- Article 子表字段 ---
    @NotBlank(message = "文章内容不能为空")
    private String body; // HTML 或 Markdown 内容
    
    @NotBlank(message = "作者不能为空")
    private String author;
}