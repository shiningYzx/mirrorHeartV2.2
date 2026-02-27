package org.shiningyang.mirrorheart_v2_2.module.community.dto;

import lombok.Data;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserInfoVo;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostVo {
    private Long id;
    private String title;
    private String text;
    private Integer visibility;
    
    // 作者信息
    private UserInfoVo author;

    // 媒体资源
    private List<String> imageUrls;
    private String audioUrl;
    private Integer audioDurationMs;

    // 互动数据
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer viewCount;
    private Long heatScore;


    private Boolean isLiked = false; // 🌟 当前登录用户是否已点赞

    private Boolean isFavorited = false; // 🌟 顺便把是否已收藏也加上，方便前端使用

    private LocalDateTime createdAt;
}