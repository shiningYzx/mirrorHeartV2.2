package org.shiningyang.mirrorheart_v2_2.module.interaction.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserInfoVo;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.shiningyang.mirrorheart_v2_2.module.community.entity.Post;
import org.shiningyang.mirrorheart_v2_2.module.interaction.dto.BlockToggleDto;
import org.shiningyang.mirrorheart_v2_2.module.interaction.dto.FavoriteToggleDto;
import org.shiningyang.mirrorheart_v2_2.module.interaction.dto.FollowToggleDto;
import org.shiningyang.mirrorheart_v2_2.module.interaction.dto.LikeToggleDto;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.FavoriteAction;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.UserRelation;
import org.shiningyang.mirrorheart_v2_2.module.interaction.service.IFavoriteActionService;
import org.shiningyang.mirrorheart_v2_2.module.interaction.service.ILikeActionService;
import org.shiningyang.mirrorheart_v2_2.module.interaction.service.IUserRelationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interaction")
@RequiredArgsConstructor
public class InteractionController {

    private final ILikeActionService likeService;
    private final IFavoriteActionService favoriteService;
    private final IUserRelationService relationService; // 注入最新的单表关系服务

    // 点赞/取消点赞
    @PostMapping("/like")
    public Result<Boolean> toggleLike(@RequestBody @Valid LikeToggleDto dto) {
        Long userId = SecurityUtils.getUserId();
        boolean isLiked = likeService.toggleLike(userId, dto);
        return Result.success(isLiked);
    }

    // 收藏/取消收藏 (帖子)
    @PostMapping("/favorite")
    public Result<Boolean> toggleFavorite(@RequestBody @Valid FavoriteToggleDto dto) {
        Long userId = SecurityUtils.getUserId();
        boolean isFavorited = favoriteService.toggleFavorite(userId, dto);
        return Result.success(isFavorited);
    }

    // 关注或取消关注
    @PostMapping("/follow")
    public Result<Boolean> toggleFollow(@RequestBody @Valid FollowToggleDto dto) {
        Long userId = SecurityUtils.getUserId();
        boolean isFollowed = relationService.toggleFollow(userId, dto);
        return Result.success(isFollowed);
    }

    /**
     * [新增] 获取我收藏的帖子列表
     */
    @GetMapping("/favorite/my-post-list")
    public Result<IPage<Post>> getMyFavoritePosts(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Long currentUserId = SecurityUtils.getUserId();
        Page<FavoriteAction> page = new Page<>(pageNo, pageSize);
        return Result.success(favoriteService.getMyFavoritePosts(currentUserId, page));
    }

    // 拉黑或取消拉黑
    @PostMapping("/block")
    public Result<Boolean> toggleBlock(@RequestBody @Valid BlockToggleDto dto) {
        Long userId = SecurityUtils.getUserId();
        boolean isBlocked = relationService.toggleBlock(userId, dto);
        return Result.success(isBlocked);
    }

    // 获取我的关注列表
    @GetMapping("/follow/my-list")
    public Result<IPage<User>> getMyFollowedList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long currentUserId = SecurityUtils.getUserId();
        Page<UserRelation> page = new Page<>(pageNo, pageSize);
        Result<IPage<User>> result = Result.success(relationService.getMyFollowedList(currentUserId, page));
        if (result.getData().getRecords().isEmpty()) {
            result.setMessage("暂无关注");
        }
        return  result;
    }

    // 🌟 新增：获取特定用户的关注列表 (主页用，带隐私过滤)
    @GetMapping("/user/{userId}/following")
    public Result<IPage<UserInfoVo>> getUserFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Long currentUserId = null;
        try { currentUserId = SecurityUtils.getUserId(); } catch (Exception ignored) {}

        Page<UserRelation> page = new Page<>(pageNo, pageSize);
        return Result.success(relationService.getUserFollowedList(userId, currentUserId, page));
    }

    // 🌟 新增：获取特定用户的粉丝列表 (主页用，带隐私过滤)
    @GetMapping("/user/{userId}/followers")
    public Result<IPage<UserInfoVo>> getUserFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Long currentUserId = null;
        try { currentUserId = SecurityUtils.getUserId(); } catch (Exception ignored) {}

        Page<UserRelation> page = new Page<>(pageNo, pageSize);
        return Result.success(relationService.getUserFollowerList(userId, currentUserId, page));
    }

    // 获取我的黑名单列表
    @GetMapping("/block/my-list")
    public Result<IPage<UserInfoVo>> getMyBlockedList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        // 黑名单属于绝对隐私数据，必须是登录用户自己才能获取
        Long currentUserId = SecurityUtils.getUserId();

        Page<UserRelation> page = new Page<>(pageNo, pageSize);
        IPage<UserInfoVo> resultPage = relationService.getMyBlockedList(currentUserId, page);

        return Result.success(resultPage);
    }
}