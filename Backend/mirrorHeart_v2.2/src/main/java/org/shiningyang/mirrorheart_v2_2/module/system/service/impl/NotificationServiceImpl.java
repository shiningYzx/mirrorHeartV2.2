package org.shiningyang.mirrorheart_v2_2.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserInfoVo;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.shiningyang.mirrorheart_v2_2.module.auth.service.IUserService;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.UserRelation;
import org.shiningyang.mirrorheart_v2_2.module.interaction.mapper.UserRelationMapper;
import org.shiningyang.mirrorheart_v2_2.module.system.dto.NotificationVo;
import org.shiningyang.mirrorheart_v2_2.module.system.entity.Notification;
import org.shiningyang.mirrorheart_v2_2.module.system.mapper.NotificationMapper;
import org.shiningyang.mirrorheart_v2_2.module.system.service.INotificationService;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j // 加上这个注解，Lombok 会自动为您生成正确的 log 对象
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {
    private final IUserService userService;
    private final UserRelationMapper userRelationMapper; // 🌟 注入新的关系Mapper

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNotification(Long receiverId, Long senderId, String type, String targetType, Long targetId, String content) {
        // 1. 如果是自己对自己操作（如点赞自己的帖子），不发通知
        if (receiverId.equals(senderId)) {
            return;
        }

        // 2. 插入数据库
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setSenderId(senderId);
        notification.setType(type);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        // 内容过长截取一下，防止数据库报错
        if (content != null && content.length() > 100) {
            content = content.substring(0, 100) + "...";
        }
        notification.setContent(content == null ? "" : content);
        notification.setIsRead((byte) 0); // 0=未读
        this.save(notification);
    }

    @Override
    @Async
    public void notifyFollowersOnNewPost(Long authorId, Long postId, String textContent) {
        log.info("开始为博主 {} 的新帖异步生成粉丝通知...", authorId);

        // 🌟【核心修改】：从 user_relation 表查询目标为该博主，且类型为 1(关注) 的粉丝
        List<UserRelation> fansList = userRelationMapper.selectList(new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getToUserId, authorId)
                .eq(UserRelation::getType, 1));

        if (fansList == null || fansList.isEmpty()) return;

        List<Notification> notifications = fansList.stream().map(fan -> {
            Notification n = new Notification();
            n.setReceiverId(fan.getFromUserId()); // 接收者是发起关注的粉丝
            n.setSenderId(authorId);
            n.setType("NEW_POST");
            n.setTargetType("POST");
            n.setTargetId(postId);
            String snippet = textContent != null && textContent.length() > 30 ? textContent.substring(0, 30) + "..." : textContent;
            n.setContent(snippet == null ? "发布了新动态" : snippet);
            n.setIsRead((byte) 0);
            n.setCreatedAt(LocalDateTime.now());
            return n;
        }).collect(Collectors.toList());

        this.saveBatch(notifications);
        log.info("异步通知完成，共向 {} 位粉丝发送了通知。", notifications.size());
    }


    @Override
    public Long getUnreadCount(Long userId) {
        return this.count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, 0));
    }

    @Override
    public IPage<NotificationVo> getNotificationList(Page<Notification> page, Long userId) {
        // 1. 查询该用户的通知，按时间倒序
        Page<Notification> resultPage = this.page(page, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getReceiverId, userId)
                .orderByDesc(Notification::getCreatedAt));

        // 2. 转换为 VO，填充发送者信息
        return resultPage.convert(n -> {
            NotificationVo vo = new NotificationVo();
            BeanUtils.copyProperties(n, vo);
            vo.setIsRead(n.getIsRead().intValue());

            if (n.getSenderId() != null) {
                User sender = userService.getById(n.getSenderId());
                if (sender != null) {
                    vo.setSender(UserInfoVo.fromUser(sender));
                }
            }
            return vo;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        // 将该用户所有未读消息更新为已读
        this.lambdaUpdate()
                .set(Notification::getIsRead, 1)
                .eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, 0)
                .update();
    }
}