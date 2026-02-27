package org.shiningyang.mirrorheart_v2_2.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 集中存放 Auth 相关的辅助 DTO (为了减少文件数量整合在一个类中)
 */
public class AuthActionDtos {

    @Data
    public static class SendOtpDto {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;
        
        @NotBlank(message = "场景不能为空(REGISTER, LOGIN, RESET, CANCEL)")
        private String scene;
    }

    @Data
    public static class EmailLoginDto {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "验证码不能为空")
        private String code;
    }

    @Data
    public static class ResetPwdDto {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "验证码不能为空")
        private String code;

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 20, message = "新密码长度需在6-20位之间")
        private String newPassword;
    }

    @Data
    public static class CancelAccountDto {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "验证码不能为空")
        private String code;

        @NotBlank(message = "原因不能为空")
        private String reason;
    }

    // [新增] 找回账号 DTO
    @Data
    public static class RecoverAccountDto {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String email;

        @NotBlank(message = "验证码不能为空")
        private String code;
    }
}