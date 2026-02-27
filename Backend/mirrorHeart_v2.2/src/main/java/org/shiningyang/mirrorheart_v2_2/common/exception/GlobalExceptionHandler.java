package org.shiningyang.mirrorheart_v2_2.common.exception;

import org.shiningyang.mirrorheart_v2_2.common.result.ErrorCode;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 捕获自定义业务异常
    @ExceptionHandler(CustomException.class)
    public Result<?> handleCustomException(CustomException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // 2. 捕获参数校验异常 (Validation)
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        log.warn("参数校验异常: {}", e.getMessage());
        String msg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    // 3. 捕获所有未知的系统异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统未知异常", e);
        return Result.error(ErrorCode.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后再试");
    }

    // [新增] 单独处理 404 错误，不要当成系统异常
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<?> handleNoResourceFoundException(NoResourceFoundException e) {
        // 不需要打印堆栈，只记录警告
        log.warn("接口不存在: {}", e.getResourcePath());
        return Result.error(404, "接口不存在: " + e.getResourcePath());
    }
}