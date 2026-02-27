package org.shiningyang.mirrorheart_v2_2.module.admin.controller;

import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.annotation.AuditLog;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.auth.service.IUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端：用户与权限治理 API
 */
@RestController
@RequestMapping("/api/v1/admin/user")
@RequiredArgsConstructor
public class AdminUserController {

    private final IUserService userService;

    /**
     * 赋予普通用户管理员权限
     */
    @PutMapping("/{id}/grant-admin")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "用户管理", operation = "赋予管理员权限")
    public Result<String> grantAdmin(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getUserId();
        userService.grantAdmin(currentUserId, id);
        return Result.success("赋予管理员权限成功");
    }

    /**
     * 收回管理员权限 (降级为普通用户)
     */
    @PutMapping("/{id}/revoke-admin")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "用户管理", operation = "收回管理员权限")
    public Result<String> revokeAdmin(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getUserId();
        userService.revokeAdmin(currentUserId, id);
        return Result.success("收回管理员权限成功，该用户已被强制下线");
    }
}