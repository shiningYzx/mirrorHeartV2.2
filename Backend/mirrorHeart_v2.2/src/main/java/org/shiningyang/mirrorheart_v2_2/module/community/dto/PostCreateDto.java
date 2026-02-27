package org.shiningyang.mirrorheart_v2_2.module.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PostCreateDto {

    @Size(max = 255, message = "标题不能超过255字")
    private String title;

    @Size(max = 2000, message = "正文不能超过2000字")
    @NotBlank(message = "正文不能为空")
    private String text;

    // 0=公开, 1=粉丝可见, 2=私密
    private Integer visibility = 0;

    // 图片列表 (URL)
    private List<String> imageUrls;

    // 音频 (可选)
    private String audioUrl;
    private Integer audioDurationMs;

    // 标签ID列表
    private List<Long> tagIds;
}