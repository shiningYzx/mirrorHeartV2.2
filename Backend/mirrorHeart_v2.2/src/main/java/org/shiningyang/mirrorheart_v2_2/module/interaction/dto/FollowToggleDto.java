package org.shiningyang.mirrorheart_v2_2.module.interaction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FollowToggleDto {
    @NotNull(message = "关注目标用户ID不能为空")
    private Long targetUserId;
}