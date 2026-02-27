package org.shiningyang.mirrorheart_v2_2.common.exception;

import org.shiningyang.mirrorheart_v2_2.common.result.ErrorCode;
import org.shiningyang.mirrorheart_v2_2.common.result.IErrorCode;
import lombok.Getter;

/**
 * 自定义业务异常
 * 用法：throw new CustomException(ErrorCode.USER_NOT_EXIST);
 */
@Getter
public class CustomException extends RuntimeException {
    
    private final Integer code;
    
    public CustomException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public CustomException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    public CustomException(String message) {
        super(message);
        this.code = ErrorCode.SYSTEM_ERROR.getCode();
    }
}