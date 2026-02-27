package org.shiningyang.mirrorheart_v2_2.module.interaction.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.shiningyang.mirrorheart_v2_2.module.community.entity.Post;
import org.shiningyang.mirrorheart_v2_2.module.interaction.dto.FavoriteToggleDto;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.FavoriteAction;

public interface IFavoriteActionService extends IService<FavoriteAction> {
    boolean toggleFavorite(Long userId, FavoriteToggleDto dto);

    /**
     * [新增] 获取我收藏的帖子列表
     */
    IPage<Post> getMyFavoritePosts(Long userId, Page<FavoriteAction> pageParam);
}