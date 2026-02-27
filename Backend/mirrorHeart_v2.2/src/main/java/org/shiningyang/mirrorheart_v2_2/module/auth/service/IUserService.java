package org.shiningyang.mirrorheart_v2_2.module.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserInfoVo;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;

/**
 * <p>
 * 用户基础表 服务类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
public interface IUserService extends IService<User> {
    /**
     * 获取用户信息 (走 Redis 缓存)
     */
    User getUserInfo(Long userId);

    /**
     * 全量更新用户信息 (清除对应用户的缓存)
     */
    void updateUserInfo(User user);

    // 🌟 新增：单独修改头像
    void updateAvatar(Long userId, String avatarUrl);

    // 🌟 新增：单独修改生日 (每年限一次)
    void updateBirthday(Long userId, LocalDate birthday);

    /**
     * 赋予管理员权限
     */
    void grantAdmin(Long currentUserId, Long targetUserId);

    /**
     * 收回管理员权限 (并强制下线)
     */
    void revokeAdmin(Long currentUserId, Long targetUserId);

    /**
     * 搜索用户
     */
    IPage<UserInfoVo> searchUsers(Page<User> page, String keyword, Long currentUserId);

    // 新增：根据用户ID获取公开的主页信息
    UserInfoVo getUserProfile(Long targetUserId, Long currentUserId);
}
