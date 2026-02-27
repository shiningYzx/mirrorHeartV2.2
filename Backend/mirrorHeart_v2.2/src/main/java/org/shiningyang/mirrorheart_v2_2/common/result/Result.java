package org.shiningyang.mirrorheart_v2_2.common.result;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一 API 响应结果封装
 */
@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(0); // 0 表示成功
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(int code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(msg);
        result.setData(null);
        return result;
    }
    
    // 配合 ErrorCode 枚举使用
    public static <T> Result<T> error(IErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }
}