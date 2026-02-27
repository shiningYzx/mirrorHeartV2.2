package org.shiningyang.mirrorheart_v2_2.module.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.shiningyang.mirrorheart_v2_2.module.system.dto.NotificationVo;
import org.shiningyang.mirrorheart_v2_2.module.system.entity.Notification;

public interface INotificationService extends IService<Notification> {
    /**
     * 创建一条通知
     */
    void createNotification(Long receiverId, Long senderId, String type, String targetType, Long targetId, String content);

    // [新增] 异步发送粉丝新动态通知
    void notifyFollowersOnNewPost(Long authorId, Long postId, String textContent);

    Long getUnreadCount(Long userId);

    IPage<NotificationVo> getNotificationList(Page<Notification> page, Long userId);

    void markAllAsRead(Long userId);
}