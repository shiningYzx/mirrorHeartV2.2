package org.shiningyang.mirrorheart_v2_2.module.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.EmailOtp;

public interface IEmailOtpService extends IService<EmailOtp> {
    
    /**
     * 发送邮箱验证码
     * @param email 接收邮箱
     * @param scene 场景 (REGISTER, LOGIN, RESET, CANCEL)
     */
    void sendOtp(String email, String scene);

    /**
     * 校验验证码 (校验成功后会自动标记为已使用)
     * @param email 邮箱
     * @param scene 场景
     * @param code 验证码
     */
    void verifyOtp(String email, String scene, String code);
}