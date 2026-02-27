package org.shiningyang.mirrorheart_v2_2.module.content.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContentListVo {
    private Long id;
    private String type;        // QUOTE, BOOK, MOVIE...
    private String title;
    private String summary;
    private String coverUrl;
    private String source;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private Boolean isLiked;
}