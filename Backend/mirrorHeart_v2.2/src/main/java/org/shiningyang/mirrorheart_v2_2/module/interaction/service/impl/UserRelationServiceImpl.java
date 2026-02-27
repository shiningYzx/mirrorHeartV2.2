package org.shiningyang.mirrorheart_v2_2.module.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserInfoVo;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.shiningyang.mirrorheart_v2_2.module.auth.mapper.UserMapper;
import org.shiningyang.mirrorheart_v2_2.module.interaction.dto.BlockToggleDto;
import org.shiningyang.mirrorheart_v2_2.module.interaction.dto.FollowToggleDto;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.UserRelation;
import org.shiningyang.mirrorheart_v2_2.module.interaction.mapper.UserRelationMapper;
import org.shiningyang.mirrorheart_v2_2.module.interaction.service.IUserRelationService;
import org.shiningyang.mirrorheart_v2_2.module.system.service.INotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRelationServiceImpl extends ServiceImpl<UserRelationMapper, UserRelation> implements IUserRelationService {

    private final UserMapper userMapper;
    private final INotificationService notificationService; // 注入通知服务

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFollow(Long currentUserId, FollowToggleDto dto) {
        Long targetId = dto.getTargetUserId();
        if (currentUserId.equals(targetId)) throw new CustomException("不能关注自己");
        checkUserExists(targetId);

        // 互斥校验：是否已经拉黑了对方？拉黑状态下不允许关注
        boolean isBlocking = this.baseMapper.exists(new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getFromUserId, currentUserId)
                .eq(UserRelation::getToUserId, targetId)
                .eq(UserRelation::getType, 2));
        if (isBlocking) {
            throw new CustomException("您已拉黑该用户，请先解除拉黑再关注");
        }

        // 查询关注关系 (type=1)
        UserRelation relation = this.getOne(new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getFromUserId, currentUserId)
                .eq(UserRelation::getToUserId, targetId)
                .eq(UserRelation::getType, 1));

        boolean isFollow;
        if (relation != null) {
            this.removeById(relation.getId());
            isFollow = false;
        } else {
            relation = new UserRelation();
            relation.setFromUserId(currentUserId);
            relation.setToUserId(targetId);
            relation.setType((byte) 1); // 1 = 关注
            this.save(relation);
            isFollow = true;
            
            // 发送系统通知
            notificationService.createNotification(
                    targetId, currentUserId, "FOLLOW", "USER", currentUserId, "关注了你"
            );
        }
        return isFollow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleBlock(Long currentUserId, BlockToggleDto dto) {
        Long targetId = dto.getTargetUserId();
        if (currentUserId.equals(targetId)) throw new CustomException("不能拉黑自己");
        checkUserExists(targetId);

        // 查询拉黑关系 (type=2)
        UserRelation relation = this.getOne(new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getFromUserId, currentUserId)
                .eq(UserRelation::getToUserId, targetId)
                .eq(UserRelation::getType, 2));

        boolean isBlocked;
        if (relation != null) {
            this.removeById(relation.getId());
            isBlocked = false;
        } else {
            relation = new UserRelation();
            relation.setFromUserId(currentUserId);
            relation.setToUserId(targetId);
            relation.setType((byte) 2); // 2 = 拉黑
            this.save(relation);
            isBlocked = true;

            // 业务联动逻辑：拉黑对方时，自动双向解除双方的关注关系 (斩断瓜葛)
            this.baseMapper.delete(new LambdaQueryWrapper<UserRelation>()
                    .eq(UserRelation::getType, 1) // 类型必须是关注
                    .and(wrapper -> wrapper
                            .nested(w -> w.eq(UserRelation::getFromUserId, currentUserId).eq(UserRelation::getToUserId, targetId))
                            .or()
                            .nested(w -> w.eq(UserRelation::getFromUserId, targetId).eq(UserRelation::getToUserId, currentUserId))
                    ));
        }
        return isBlocked;
    }

    @Override
    public IPage<User> getMyFollowedList(Long currentUserId, Page<UserRelation> pageParam) {
        // 查询 fromUserId 是我，且 type = 1(关注) 的关系
        IPage<UserRelation> relationPage = this.baseMapper.selectPage(pageParam, new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getFromUserId, currentUserId)
                .eq(UserRelation::getType, 1)
                .orderByDesc(UserRelation::getCreatedAt));

        // 提取目标用户 ID
        List<Long> followedIds = relationPage.getRecords().stream()
                .map(UserRelation::getToUserId)
                .collect(Collectors.toList());

        Page<User> userPage = new Page<>(relationPage.getCurrent(), relationPage.getSize(), relationPage.getTotal());
        
        if (!followedIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(followedIds);
            // 隐藏密码等敏感信息
            users.forEach(u -> u.setPasswordHash(null));
            userPage.setRecords(users);
        }
        
        return userPage;
    }

    // 🌟 1. 获取目标用户的关注列表，并带上隐私校验
    @Override
    public IPage<UserInfoVo> getUserFollowedList(Long targetUserId, Long currentUserId, Page<UserRelation> pageParam) {
        checkPrivacy(targetUserId, currentUserId, User::getShowFollowing, "关注列表");

        IPage<UserRelation> relationPage = this.baseMapper.selectPage(pageParam, new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getFromUserId, targetUserId)
                .eq(UserRelation::getType, 1)
                .orderByDesc(UserRelation::getCreatedAt));

        return buildUserInfoVoPage(relationPage, UserRelation::getToUserId, currentUserId);
    }

    // 🌟 2. 获取目标用户的粉丝列表，并带上隐私校验
    @Override
    public IPage<UserInfoVo> getUserFollowerList(Long targetUserId, Long currentUserId, Page<UserRelation> pageParam) {
        checkPrivacy(targetUserId, currentUserId, User::getShowFollowing, "粉丝列表");

        IPage<UserRelation> relationPage = this.baseMapper.selectPage(pageParam, new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getToUserId, targetUserId)
                .eq(UserRelation::getType, 1)
                .orderByDesc(UserRelation::getCreatedAt));

        return buildUserInfoVoPage(relationPage, UserRelation::getFromUserId, currentUserId);
    }

    // 获取我的拉黑列表
    @Override
    public IPage<UserInfoVo> getMyBlockedList(Long currentUserId, Page<UserRelation> pageParam) {
        // 1. 去关系表里查我主动拉黑的人 (type = 2 代表拉黑)
        IPage<UserRelation> relationPage = this.baseMapper.selectPage(pageParam, new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getFromUserId, currentUserId)
                .eq(UserRelation::getType, 2)
                .orderByDesc(UserRelation::getCreatedAt));

        Page<UserInfoVo> voPage = new Page<>(relationPage.getCurrent(), relationPage.getSize(), relationPage.getTotal());

        List<Long> blockedUserIds = relationPage.getRecords().stream()
                .map(UserRelation::getToUserId)
                .collect(Collectors.toList());

        if (blockedUserIds.isEmpty()) {
            return voPage;
        }

        // 2. 批量查出被拉黑用户的基本信息，并用 fromUser 屏蔽掉私密字段
        List<User> users = userMapper.selectBatchIds(blockedUserIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        List<UserInfoVo> voList = blockedUserIds.stream()
                .map(id -> {
                    User u = userMap.get(id);
                    if (u != null) {
                        return UserInfoVo.fromUser(u);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 隐私通用校验
     */
    private void checkPrivacy(Long targetUserId, Long currentUserId, Function<User, Byte> privacyField, String listName) {
        User targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) throw new CustomException("目标用户不存在");

        boolean isSelf = currentUserId != null && currentUserId.equals(targetUserId);
        Byte isShow = privacyField.apply(targetUser);

        if (!isSelf && isShow != null && isShow == 0) {
            throw new CustomException("该用户已隐藏" + listName);
        }
    }

    /**
     * 将关联数据批量转换为带 isFollowed 状态的 UserInfoVo
     */
    private IPage<UserInfoVo> buildUserInfoVoPage(IPage<UserRelation> relationPage, Function<UserRelation, Long> idExtractor, Long currentUserId) {
        List<Long> userIds = relationPage.getRecords().stream().map(idExtractor).collect(Collectors.toList());

        Page<UserInfoVo> voPage = new Page<>(relationPage.getCurrent(), relationPage.getSize(), relationPage.getTotal());
        if (userIds.isEmpty()) return voPage;

        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        // 批量查询当前登录用户是否关注了列表中的这些人
        Set<Long> followedSet = new HashSet<>();
        if (currentUserId != null) {
            List<UserRelation> myFollows = this.baseMapper.selectList(new LambdaQueryWrapper<UserRelation>()
                    .eq(UserRelation::getFromUserId, currentUserId)
                    .in(UserRelation::getToUserId, userIds)
                    .eq(UserRelation::getType, 1));
            followedSet = myFollows.stream().map(UserRelation::getToUserId).collect(Collectors.toSet());
        }

        final Set<Long> finalFollowedSet = followedSet;
        List<UserInfoVo> voList = userIds.stream().map(id -> {
            User user = userMap.get(id);
            if (user == null) return null;
            UserInfoVo vo = UserInfoVo.fromUser(user);
            vo.setIsFollowed(finalFollowedSet.contains(id));
            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    private void checkUserExists(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new CustomException("目标用户不存在或状态异常");
        }
    }
}