package org.shiningyang.mirrorheart_v2_2.module.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.system.dto.NotificationVo;
import org.shiningyang.mirrorheart_v2_2.module.system.entity.Notification;
import org.shiningyang.mirrorheart_v2_2.module.system.service.INotificationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    /**
     * 获取未读消息数
     */
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount() {
        Long userId = SecurityUtils.getUserId();
        return Result.success(notificationService.getUnreadCount(userId));
    }

    /**
     * 获取通知列表
     */
    @GetMapping("/list")
    public Result<IPage<NotificationVo>> getList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = SecurityUtils.getUserId();
        Page<Notification> page = new Page<>(pageNo, pageSize);
        IPage<NotificationVo> result = notificationService.getNotificationList(page, userId);

        Result<IPage<NotificationVo>> res = Result.success(result);
        if (result.getRecords().isEmpty()) {
            res.setMessage("暂无新消息");
        }

        return res;
    }

    /**
     * 全部标记为已读
     */
    @PutMapping("/read-all")
    public Result<String> readAll() {
        Long userId = SecurityUtils.getUserId();
        notificationService.markAllAsRead(userId);
        return Result.success("已标记全部已读");
    }
}