package org.shiningyang.mirrorheart_v2_2.module.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentCreateDto {

    @NotNull(message = "帖子ID不能为空")
    private Long postId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论不能超过500字")
    private String text;

    // 如果是回复某条评论，填这个 (父评论ID)
    private Long parentId;

    // 如果是楼中楼，填这个 (根评论ID)。一级评论则为空。
    private Long rootId;
}