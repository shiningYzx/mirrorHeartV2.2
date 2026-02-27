package org.shiningyang.mirrorheart_v2_2.module.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserInfoVo;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.shiningyang.mirrorheart_v2_2.module.auth.service.IUserService;
import org.shiningyang.mirrorheart_v2_2.module.community.dto.PostVo;
import org.shiningyang.mirrorheart_v2_2.module.community.entity.Post;
import org.shiningyang.mirrorheart_v2_2.module.community.service.IPostService;
import org.shiningyang.mirrorheart_v2_2.module.system.service.SearchHistoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final IPostService postService;
    private final IUserService userService;
    private final SearchHistoryService searchHistoryService;

    /**
     * 1. 搜索帖子
     */
    @GetMapping("/posts")
    public Result<IPage<PostVo>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
            
        Long currentUserId = getCurrentUserIdSafe();
        
        // 异步记录搜索历史 (未登录不记录)
        if (currentUserId != null) {
            searchHistoryService.addHistoryAsync(currentUserId, keyword);
        }

        Page<Post> page = new Page<>(pageNo, pageSize);
        IPage<PostVo> result = postService.searchPosts(page, keyword, currentUserId);
        
        return Result.success(result);
    }

    /**
     * 2. 搜索用户
     */
    @GetMapping("/users")
    public Result<IPage<UserInfoVo>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Long currentUserId = getCurrentUserIdSafe();
        
        // 异步记录搜索历史
        if (currentUserId != null) {
            searchHistoryService.addHistoryAsync(currentUserId, keyword);
        }

        Page<User> page = new Page<>(pageNo, pageSize);
        IPage<UserInfoVo> result = userService.searchUsers(page, keyword, currentUserId);
        
        return Result.success(result);
    }

    /**
     * 3. 获取我的搜索历史记录
     */
    @GetMapping("/history")
    public Result<List<String>> getMyHistory() {
        Long currentUserId = SecurityUtils.getUserId();
        return Result.success(searchHistoryService.getMyHistory(currentUserId));
    }

    /**
     * 4. 清空搜索历史
     */
    @DeleteMapping("/history")
    public Result<String> clearMyHistory() {
        Long currentUserId = SecurityUtils.getUserId();
        searchHistoryService.clearMyHistory(currentUserId);
        return Result.success("清除成功");
    }

    /**
     * 安全获取当前用户 ID，允许未登录时返回 null 进行搜索
     */
    private Long getCurrentUserIdSafe() {
        try {
            return SecurityUtils.getUserId();
        } catch (Exception e) {
            return null; // 未登录状态
        }
    }
}