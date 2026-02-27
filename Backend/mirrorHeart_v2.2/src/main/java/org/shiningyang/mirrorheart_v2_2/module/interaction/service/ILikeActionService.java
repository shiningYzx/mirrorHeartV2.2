package org.shiningyang.mirrorheart_v2_2.module.interaction.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.shiningyang.mirrorheart_v2_2.module.interaction.dto.LikeToggleDto;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.LikeAction;

public interface ILikeActionService extends IService<LikeAction> {
    boolean toggleLike(Long userId, LikeToggleDto dto);
}