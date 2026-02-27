package org.shiningyang.mirrorheart_v2_2.module.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class BookCreateDto {
    
    @NotBlank(message = "书名不能为空")
    private String title;
    
    private String summary;
    private String coverUrl;
    private List<Long> tagIds;

    // --- Book 子表字段 ---
    private String isbn;
    
    @NotBlank(message = "作者不能为空")
    private String author;
    
    private String publisher; // 出版社
}