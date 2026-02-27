package org.shiningyang.mirrorheart_v2_2.module.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class MusicCreateDto {
    
    @NotBlank(message = "音乐标题不能为空")
    private String title;
    
    private String summary;
    private String coverUrl; // 专辑封面
    private List<Long> tagIds;

    // --- Music 子表字段 ---
    @NotBlank(message = "艺术家/歌手不能为空")
    private String artist;
    
    @NotBlank(message = "音频链接不能为空")
    private String audioUrl;
    
    private Integer durationMs; // 时长(毫秒)
    private String lyric; // 歌词
}