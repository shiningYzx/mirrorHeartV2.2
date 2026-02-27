package org.shiningyang.mirrorheart_v2_2.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserInfoVo;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.AuthSession;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.shiningyang.mirrorheart_v2_2.module.auth.mapper.AuthSessionMapper;
import org.shiningyang.mirrorheart_v2_2.module.auth.mapper.UserMapper;
import org.shiningyang.mirrorheart_v2_2.module.auth.service.IUserService;
import org.shiningyang.mirrorheart_v2_2.module.community.entity.Post;
import org.shiningyang.mirrorheart_v2_2.module.community.mapper.PostMapper;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.UserRelation;
import org.shiningyang.mirrorheart_v2_2.module.interaction.mapper.UserRelationMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final AuthSessionMapper authSessionMapper; // 用于强制用户下线
    private final UserRelationMapper userRelationMapper;
    // 注入 PostMapper 用于统计帖子数
    private final PostMapper postMapper;

    @Override
    @Cacheable(value = "user:info", key = "#userId")
    public User getUserInfo(Long userId) {
        return this.getById(userId);
    }

    // 全量更新用户信息
    @Override
    @CacheEvict(value = "user:info", key = "#user.id")
    public void updateUserInfo(User user) {
        this.updateById(user);
    }

    @Override
    public IPage<UserInfoVo> searchUsers(Page<User> pageParam, String keyword, Long currentUserId) {
        // 1. 根据用户名模糊查询
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, 1);
        wrapper.like(User::getNickname, keyword); // 可以根据需要增加 .or().like(User::getBio, keyword)

        Page<User> userPage = this.page(pageParam, wrapper);
        IPage<UserInfoVo> voPage = userPage.convert(UserInfoVo::fromUser);

        // 🌟 2. 批量装配这些被搜出来的用户，是否被当前登录者关注过
        if (currentUserId != null && !voPage.getRecords().isEmpty()) {
            Set<Long> userIds = voPage.getRecords().stream()
                    .map(UserInfoVo::getId).collect(Collectors.toSet());

            Set<Long> followedIds = userRelationMapper.selectList(new LambdaQueryWrapper<UserRelation>()
                    .eq(UserRelation::getFromUserId, currentUserId)
                    .eq(UserRelation::getType, 1)
                    .in(UserRelation::getToUserId, userIds)
            ).stream().map(UserRelation::getToUserId).collect(Collectors.toSet());

            voPage.getRecords().forEach(vo -> {
                if (vo.getId().equals(currentUserId)) {
                    vo.setIsFollowed(false);
                } else {
                    vo.setIsFollowed(followedIds.contains(vo.getId()));
                }
            });
        }
        return voPage;
    }

    // 新增：获取特定用户的公开主页信息
    @Override
    public UserInfoVo getUserProfile(Long targetUserId, Long currentUserId) {
        // 1. 查询目标用户信息
        User targetUser = this.getById(targetUserId);
        if (targetUser == null || targetUser.getStatus() != 1) {
            throw new CustomException("该用户不存在或状态异常");
        }

        // 2. 🌟 区分是否是本人，采用不同的序列化策略保护隐私
        boolean isSelf = currentUserId != null && currentUserId.equals(targetUserId);
        UserInfoVo vo = isSelf ? UserInfoVo.fromSelf(targetUser) : UserInfoVo.fromUser(targetUser);

        // 3. 核心扩展：查验关注状态
        if (currentUserId != null) {
            boolean isFollowed = userRelationMapper.exists(new LambdaQueryWrapper<UserRelation>()
                    .eq(UserRelation::getFromUserId, currentUserId)
                    .eq(UserRelation::getToUserId, targetUserId)
                    .eq(UserRelation::getType, 1)); // type=1 表示处于关注状态
            vo.setIsFollowed(isFollowed);
        } else {
            // 游客未登录，默认未关注
            vo.setIsFollowed(false);
        }

        // 4. 🌟 进阶扩展：统计帖子数、关注数、粉丝数
        // 统计帖子数 (排除私密帖子，这里假设 visibility=2 是私密)
        long postCount = postMapper.selectCount(new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, targetUserId)
                .ne(Post::getVisibility, 2));
        vo.setPostCount((int) postCount);

        // 统计关注数 (该用户作为 FromUserId 去关注别人)
        long followingCount = userRelationMapper.selectCount(new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getFromUserId, targetUserId)
                .eq(UserRelation::getType, 1));
        vo.setFollowingCount((int) followingCount);

        // 统计粉丝数 (该用户作为 ToUserId 被别人关注)
        long followerCount = userRelationMapper.selectCount(new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getToUserId, targetUserId)
                .eq(UserRelation::getType, 1));
        vo.setFollowerCount((int) followerCount);

        return vo;
    }

    // 新增：单独修改头像
    @Override
    @CacheEvict(value = "user:info", key = "#userId")
    public void updateAvatar(Long userId, String avatarUrl) {
        User updateObj = new User();
        updateObj.setId(userId);
        updateObj.setAvatarUrl(avatarUrl);
        this.updateById(updateObj);
    }

    // 新增：单独修改生日
    @Override
    @CacheEvict(value = "user:info", key = "#userId")
    public void updateBirthday(Long userId, LocalDate birthday) {
        User user = this.getById(userId);
        if (user == null) {
            throw new CustomException("用户不存在");
        }

        // 🌟 核心拦截：校验每年只能修改一次
        if (user.getBirthdayUpdateTime() != null) {
            int lastUpdateYear = user.getBirthdayUpdateTime().getYear();
            int currentYear = LocalDate.now().getYear();
            if (lastUpdateYear == currentYear) {
                throw new CustomException("为了防止频繁变动，生日信息每年只能修改一次哦");
            }
        }

        User updateObj = new User();
        updateObj.setId(userId);
        updateObj.setBirthday(birthday);
        updateObj.setBirthdayUpdateTime(LocalDateTime.now()); // 记录修改时间
        this.updateById(updateObj);
    }


    // ==========================================
    // 下方为管理员权限调配逻辑
    // ==========================================

    @Override
    @CacheEvict(value = "user:info", key = "#targetUserId") // 权限变更后踢掉其个人信息缓存
    public void grantAdmin(Long currentUserId, Long targetUserId) {
        User user = this.getById(targetUserId);
        if (user == null) {
            throw new CustomException("目标用户不存在");
        }
        if (user.getRole() == 1) {
            throw new CustomException("该用户已经是管理员，无需重复赋予");
        }
        if (user.getStatus() != 1) {
            throw new CustomException("该账号状态异常，无法被赋予管理员权限");
        }

        // 0=普通用户, 1=管理员
        user.setRole((byte) 1);
        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务保证数据一致性
    @CacheEvict(value = "user:info", key = "#targetUserId")
    public void revokeAdmin(Long currentUserId, Long targetUserId) {
        // 【防御1】：防自杀机制，不能自己撤销自己
        if (currentUserId.equals(targetUserId)) {
            throw new CustomException("您不能收回自己的管理员权限！请联系其他管理员操作。");
        }

        // 【防御2】：保护创世超管 (假设 ID 为 1 的用户为系统初创超管，绝不允许被撤销)
        if (targetUserId == 1L) {
            throw new CustomException("系统超级管理员不可被收回权限！");
        }

        User user = this.getById(targetUserId);
        if (user == null) {
            throw new CustomException("目标用户不存在");
        }
        if (user.getRole() == 0) {
            throw new CustomException("该用户本就是普通用户，无需收回");
        }

        // 1. 降级为普通用户
        user.setRole((byte) 0);
        this.updateById(user);

        // 2. 【核心安全防御】：强制降级用户下线
        // 将该用户在 auth_session 表中所有还未撤销的 Session 强制打上撤销时间戳
        AuthSession sessionUpdate = new AuthSession();
        sessionUpdate.setRevokedAt(LocalDateTime.now());

        authSessionMapper.update(sessionUpdate, new LambdaQueryWrapper<AuthSession>()
                .eq(AuthSession::getUserId, targetUserId)
                .isNull(AuthSession::getRevokedAt));
    }
}