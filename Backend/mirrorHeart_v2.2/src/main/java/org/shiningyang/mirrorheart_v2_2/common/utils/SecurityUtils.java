package org.shiningyang.mirrorheart_v2_2.common.utils;

import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.common.result.ErrorCode;
import org.shiningyang.mirrorheart_v2_2.common.security.LoginUser;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全服务工具类
 */
public class SecurityUtils {

    /**
     * 获取用户账户
     **/
    public static LoginUser getLoginUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof LoginUser) {
                return (LoginUser) principal;
            }
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }

    /**
     * 获取用户ID
     **/
    public static Long getUserId() {
        return getLoginUser().getUser().getId();
    }

    public static Long getSafeUserId() {
        if (getUserId() == null) {
            return null;
        }
        return getUserId();
    }

    /**
     * 获取当前用户信息
     */
    public static User getUser() {
        return getLoginUser().getUser();
    }
}