package org.shiningyang.mirrorheart_v2_2.module.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.BirthdayUpdateDto;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserInfoVo;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserUpdateDto;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.shiningyang.mirrorheart_v2_2.module.auth.service.IUserService;
import org.shiningyang.mirrorheart_v2_2.module.community.dto.PostVo;
import org.shiningyang.mirrorheart_v2_2.module.community.entity.Post;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @GetMapping("/me")
    public Result<UserInfoVo> getCurrentUser() {
        Long userId = SecurityUtils.getUserId();
        // 走缓存获取用户信息
        User user = userService.getUserInfo(userId);
        return Result.success(UserInfoVo.fromSelf(user));
    }

    @PutMapping("/update")
    public Result<Boolean> updateUserInfo(@RequestBody @Valid UserUpdateDto dto) {
        Long userId = SecurityUtils.getUserId();

        User updateUser = new User();
        updateUser.setId(userId);

        if (dto.getNickname() != null) updateUser.setNickname(dto.getNickname());
        if (dto.getAvatarUrl() != null) updateUser.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getBio() != null) updateUser.setBio(dto.getBio());
        if (dto.getShowFollowing() != null) updateUser.setShowFollowing(dto.getShowFollowing());
        if (dto.getShowFavorite() != null) updateUser.setShowFavorite(dto.getShowFavorite());
        if (dto.getShowPost() != null) updateUser.setShowPost(dto.getShowPost());

        // 调用重构后的服务方法，保存并触发缓存驱逐
        userService.updateUserInfo(updateUser);
        return Result.success(true);
    }

    @GetMapping("/search")
    public Result<IPage<UserInfoVo>> searchUsers(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam String keyword
    ) {
        Page<User> page = new Page<>(pageNo, pageSize);
        Long userId = SecurityUtils.getUserId();
        return Result.success(userService.searchUsers(page, keyword, userId));
    }

    // 🌟 新增：根据 ID 获取特定用户的公开信息 (精确搜索用户 / 查看他人主页)
    @GetMapping("/{userId}")
    public Result<UserInfoVo> getUserProfile(@PathVariable Long userId) {
        // 尝试获取当前登录用户 (允许游客访问，未登录则为 null)
        Long currentUserId = null;
        try {
            currentUserId = SecurityUtils.getUserId();
        } catch (Exception ignored) {
        }

        return Result.success(userService.getUserProfile(userId, currentUserId));
    }

    // 🌟 新增：单独修改头像接口
    @PutMapping("/avatar")
    public Result<String> updateAvatar(@RequestBody Map<String, String> body) {
        String avatarUrl = body.get("avatarUrl");
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            throw new CustomException("头像地址不能为空");
        }
        Long userId = SecurityUtils.getUserId();
        userService.updateAvatar(userId, avatarUrl);
        return Result.success("头像修改成功");
    }

    // 🌟 新增：单独修改生日接口
    @PutMapping("/birthday")
    public Result<String> updateBirthday(@RequestBody @Valid BirthdayUpdateDto dto) {
        Long userId = SecurityUtils.getUserId();
        userService.updateBirthday(userId, dto.getBirthday());
        return Result.success("生日设置成功");
    }
}