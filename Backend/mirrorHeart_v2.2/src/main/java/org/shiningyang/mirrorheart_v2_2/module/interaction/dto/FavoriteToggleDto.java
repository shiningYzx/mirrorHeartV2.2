package org.shiningyang.mirrorheart_v2_2.module.interaction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FavoriteToggleDto {
    @NotNull(message = "帖子ID不能为空")
    private Long postId;
}