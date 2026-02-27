package org.shiningyang.mirrorheart_v2_2.module.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.AuthActionDtos.*;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.LoginDto;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.RegisterDto;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.TokenVo;
import org.shiningyang.mirrorheart_v2_2.module.auth.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-code")
    public Result<String> sendCode(@RequestBody @Valid SendOtpDto dto) {
        authService.sendEmailCode(dto);
        return Result.success("验证码已发送，请注意查收");
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody @Valid RegisterDto dto) {
        authService.register(dto);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    public Result<TokenVo> login(@RequestBody @Valid LoginDto dto) {
        TokenVo tokenVo = authService.login(dto);
        return Result.success(tokenVo);
    }

    @PostMapping("/login-by-code")
    public Result<TokenVo> loginByCode(@RequestBody @Valid EmailLoginDto dto) {
        TokenVo tokenVo = authService.loginByCode(dto);
        return Result.success(tokenVo);
    }

    @PostMapping("/reset-password")
    public Result<String> resetPassword(@RequestBody @Valid ResetPwdDto dto) {
        authService.resetPassword(dto);
        return Result.success("密码修改成功，请重新登录");
    }

    @PostMapping("/cancel-account")
    public Result<String> cancelAccount(@RequestBody @Valid CancelAccountDto dto) {
        authService.cancelAccount(dto);
        return Result.success("账号已成功注销");
    }

    // [新增] 找回账号接口
    @PostMapping("/recover-account")
    public Result<String> recoverAccount(@RequestBody @Valid RecoverAccountDto dto) {
        authService.recoverAccount(dto);
        return Result.success("账号恢复成功，欢迎回来！您可以正常登录了");
    }

    /**
     * [新增] 使用 Refresh Token 刷新获取新的访问凭证
     * 该接口需要在 SecurityConfig 中放行 (目前 /api/v1/auth/** 已全部放行，无需额外配置)
     */
    @PostMapping("/refresh")
    public Result<TokenVo> refreshToken(@RequestParam String refreshToken) {
        return Result.success(authService.refreshToken(refreshToken));
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        authService.logout();
        return Result.success("退出登录成功");
    }
}