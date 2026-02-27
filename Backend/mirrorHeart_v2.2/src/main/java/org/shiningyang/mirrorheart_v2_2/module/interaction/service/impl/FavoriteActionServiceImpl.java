package org.shiningyang.mirrorheart_v2_2.module.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.module.community.entity.Post;
import org.shiningyang.mirrorheart_v2_2.module.community.service.IPostService;
import org.shiningyang.mirrorheart_v2_2.module.interaction.dto.FavoriteToggleDto;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.FavoriteAction;
import org.shiningyang.mirrorheart_v2_2.module.interaction.mapper.FavoriteActionMapper;
import org.shiningyang.mirrorheart_v2_2.module.interaction.service.IFavoriteActionService;
import org.shiningyang.mirrorheart_v2_2.module.system.service.INotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteActionServiceImpl extends ServiceImpl<FavoriteActionMapper, FavoriteAction> implements IFavoriteActionService {

    private final IPostService postService;
    private final INotificationService notificationService; // 🌟 注入通知服务

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(Long userId, FavoriteToggleDto dto) {
        // [优化] 校验帖子是否存在
        Post post = postService.getById(dto.getPostId());
        if (post == null) {
            throw new CustomException("收藏的帖子不存在或已被删除");
        }

        FavoriteAction fav = this.getOne(new LambdaQueryWrapper<FavoriteAction>()
                .eq(FavoriteAction::getUserId, userId)
                .eq(FavoriteAction::getPostId, dto.getPostId()));

        boolean isFav;
        if (fav != null) {
            this.removeById(fav.getId());
            // 🌟 取消收藏：数量-1，热度-5
            postService.lambdaUpdate()
                    .setSql(
                        "favorite_count = favorite_count - 1, heat_score = heat_score - 5")
                    .eq(Post::getId, dto.getPostId()).update();
            isFav = false;
        } else {
            fav = new FavoriteAction();
            fav.setUserId(userId);
            fav.setPostId(dto.getPostId());
            this.save(fav);
            
            postService.lambdaUpdate()
                    .setSql(
                        "favorite_count = favorite_count + 1, heat_score = heat_score + 5")
                    .eq(Post::getId, dto.getPostId()).update();
            isFav = true;

            // 🌟【核心】触发收藏通知
            notificationService.createNotification(
                    post.getUserId(), userId, "FAVORITE", "POST", post.getId(), "收藏了你的帖子"
            );
        }
        return isFav;
    }

    @Override
    public IPage<Post> getMyFavoritePosts(Long userId, Page<FavoriteAction> pageParam) {
        // 1. 分页查询当前用户的收藏记录，按收藏时间(ID)倒序排列
        IPage<FavoriteAction> favoritePage = this.baseMapper.selectPage(pageParam,
                new LambdaQueryWrapper<FavoriteAction>()
                        .eq(FavoriteAction::getUserId, userId)
                        .orderByDesc(FavoriteAction::getId));

        // 2. 准备返回的帖子分页对象 (复用收藏记录的 current, size, total)
        Page<Post> postPage = new Page<>(favoritePage.getCurrent(), favoritePage.getSize(), favoritePage.getTotal());

        // 提取 postIds
        List<Long> postIds = favoritePage.getRecords().stream()
                .map(FavoriteAction::getPostId)
                .collect(Collectors.toList());

        if (!postIds.isEmpty()) {
            // 3. 根据 postIds 批量查询帖子详细信息
            List<Post> posts = postService.listByIds(postIds);

            // 4. 【细节优化】：由于 listByIds 查出来的列表往往不保证顺序，我们需要在内存中把它重新按收藏的时间顺序排好
            Map<Long, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));
            List<Post> sortedPosts = postIds.stream()
                    .map(postMap::get)
                    .filter(Objects::nonNull) // 过滤掉可能已经被原作者删除的帖子
                    .collect(Collectors.toList());

            postPage.setRecords(sortedPosts);
        }

        return postPage;
    }
}