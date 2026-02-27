package org.shiningyang.mirrorheart_v2_2.module.community.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.common.utils.JwtUtils;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.community.dto.PostCreateDto;
import org.shiningyang.mirrorheart_v2_2.module.community.dto.PostVo;
import org.shiningyang.mirrorheart_v2_2.module.community.entity.Post;
import org.shiningyang.mirrorheart_v2_2.module.community.service.IPostService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/post")
@RequiredArgsConstructor
public class PostController {

    private final IPostService postService;

    // 发布帖子
    @PostMapping("/publish")
    public Result<String> publish(@RequestBody @Valid PostCreateDto dto) {
        Long userId = SecurityUtils.getUserId();
        postService.createPost(userId, dto);
        return Result.success("发布成功");
    }

    // 帖子列表 (搜索)
    @GetMapping("/search")
    public Result<IPage<PostVo>> search(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam String keyword
            ) {
        Page<Post> page = new Page<>(pageNo, pageSize);
        Long userId = SecurityUtils.getUserId();
        IPage<PostVo> resultPage = postService.searchPosts(page, keyword, userId);
        return Result.success(resultPage);
    }

    @GetMapping("/user/{userId}/favorites")
    public Result<IPage<PostVo>> getUserFavorites(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Long currentUserId = getCurrentUserIdSafe();
        Page<Post> page = new Page<>(pageNo, pageSize);
        IPage<PostVo> resultPage = postService.getUserFavoritePostList(page, userId, currentUserId);

        return Result.success(resultPage);
    }

    // 帖子列表 (最新)
    @GetMapping("/list/latest")
    public Result<IPage<PostVo>> getLatestList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Long currentUserId = null;
        try { currentUserId = SecurityUtils.getUserId(); } catch (Exception ignored) {}

        Page<Post> page = new Page<>(pageNo, pageSize);
        IPage<PostVo> resultPage = postService.getPostList(page, currentUserId, "latest");

        Result<IPage<PostVo>> result = Result.success(resultPage);
        if (resultPage.getRecords().isEmpty()) {
            result.setMessage("暂无帖子");
        }
        return result;
    }

    // 帖子列表 (热门)
    @GetMapping("/list/hot")
    public Result<IPage<PostVo>> getHotList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Long currentUserId = null;
        try { currentUserId = SecurityUtils.getUserId(); } catch (Exception ignored) {}

        Page<Post> page = new Page<>(pageNo, pageSize);
        // 传入 "hot" 标识
        IPage<PostVo> resultPage = postService.getPostList(page, currentUserId, "hot");

        Result<IPage<PostVo>> result = Result.success(resultPage);
        if (resultPage.getRecords().isEmpty()) {
            result.setMessage("暂无帖子");
        }
        return result;
    }

    // 帖子详情
    @GetMapping("/{id}")
    public Result<PostVo> getDetail(@PathVariable Long id) {
        return Result.success(postService.getPostDetail(id, SecurityUtils.getUserId()));
    }

    // 删除帖子
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        postService.deletePost(userId, id);
        return Result.success("删除成功");
    }

    // 新增：获取特定用户的帖子列表 (适用于个人主页)
    @GetMapping("/user/{userId}")
    public Result<IPage<PostVo>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        // 尝试获取当前登录用户 (允许游客访问，未登录则为 null)
        Long currentUserId = null;
        try {
            currentUserId = SecurityUtils.getUserId();
        } catch (Exception ignored) {
        }

        Page<Post> page = new Page<>(pageNo, pageSize);
        IPage<PostVo> resultPage = postService.getUserPostList(page, userId, currentUserId);

        Result<IPage<PostVo>> result = Result.success(resultPage);
        if (resultPage.getRecords().isEmpty()) {
            result.setMessage(currentUserId != null && currentUserId.equals(userId) ? "您还没有发布过帖子" : "该用户暂无动态");
        }
        return result;
    }

    // 🌟 修改：获取浏览足迹 (支持管理员查他人)
    @GetMapping("/history")
    public Result<IPage<PostVo>> getHistory(
            @RequestParam(required = false) Long userId, // 允许传入想要查询的用户ID
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Long currentUserId = SecurityUtils.getUserId(); // 必须登录

        // 如果前端没有传 userId，则默认查询自己的记录
        Long targetUserId = (userId != null) ? userId : currentUserId;

        Page<Post> page = new Page<>(pageNo, pageSize);
        IPage<PostVo> resultPage = postService.getPostViewHistoryList(page, targetUserId);

        return Result.success(resultPage);
    }

    // 🌟 新增：修改帖子可见度
    @PutMapping("/{id}/visibility")
    public Result<String> updateVisibility(
            @PathVariable Long id,
            @RequestParam Integer visibility) {
        Long userId = SecurityUtils.getUserId();
        postService.updatePostVisibility(userId, id, visibility);
        return Result.success("可见度修改成功");
    }

    /**
     * 内部工具：安全获取当前用户ID，供游客接口使用
     */
    private Long getCurrentUserIdSafe() {
        try {
            return SecurityUtils.getUserId();
        } catch (Exception e) {
            return null; // 游客身份
        }
    }
}