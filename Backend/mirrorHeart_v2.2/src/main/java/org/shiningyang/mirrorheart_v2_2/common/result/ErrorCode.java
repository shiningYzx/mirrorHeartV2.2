package org.shiningyang.mirrorheart_v2_2.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码枚举
 * 推荐规则：
 * 0: 成功
 * 1000~1999: 系统通用错误 (参数, 数据库等)
 * 2000~2999: 用户/认证错误
 * 3000~3999: 业务逻辑错误
 */
@Getter
@AllArgsConstructor
public enum ErrorCode implements IErrorCode {
    
    SUCCESS(0, "操作成功"),
    SYSTEM_ERROR(1000, "系统内部错误，请联系管理员"),
    PARAM_ERROR(1001, "参数校验失败"),
    
    // 认证相关
    UNAUTHORIZED(2001, "未登录或登录已过期"),
    FORBIDDEN(2003, "无权限访问此资源"),
    USER_NOT_EXIST(2004, "用户不存在"),
    PASSWORD_ERROR(2005, "用户名或密码错误"),
    
    // 业务相关
    DATA_NOT_EXIST(3001, "请求的数据不存在"),
    DUPLICATE_KEY(3002, "数据已存在，请勿重复操作"),
    FILE_UPLOAD_ERROR(3003, "文件上传失败");

    private final Integer code;
    private final String message;
}