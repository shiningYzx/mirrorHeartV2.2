package org.shiningyang.mirrorheart_v2_2.module.auth.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.EmailOtp;
import org.shiningyang.mirrorheart_v2_2.module.auth.mapper.EmailOtpMapper;
import org.shiningyang.mirrorheart_v2_2.module.auth.service.IEmailOtpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailOtpServiceImpl extends ServiceImpl<EmailOtpMapper, EmailOtp> implements IEmailOtpService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendOtp(String email, String scene) {
        // 1. 生成 6 位随机验证码 (利用 Hutool)
        String code = RandomUtil.randomNumbers(6);

        // 2. 存入数据库
        EmailOtp otp = new EmailOtp();
        otp.setEmail(email);
        otp.setScene(scene);
        otp.setCode(code);
        otp.setExpireAt(LocalDateTime.now().plusMinutes(5)); // 5分钟有效
        this.save(otp);

        // 3. 构建并发送邮件
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("Mirror Heart - 验证码");
        
        String actionName = getActionName(scene);
        message.setText("您正在进行【" + actionName + "】操作。\n\n" +
                "您的验证码是：" + code + "\n\n" +
                "该验证码将在 5 分钟后过期。请勿泄露给他人。");

        try {
            mailSender.send(message);
            log.info("验证码发送成功: email={}, scene={}, code={}", email, scene, code);
        } catch (Exception e) {
            log.error("验证码发送失败", e);
            throw new CustomException("邮件发送失败，请检查邮箱地址或稍后再试");
        }
    }

    @Override
    public void verifyOtp(String email, String scene, String code) {
        // 查找最新的一条未使用且未过期的验证码
        EmailOtp otp = this.getOne(new LambdaQueryWrapper<EmailOtp>()
                .eq(EmailOtp::getEmail, email)
                .eq(EmailOtp::getScene, scene)
                .eq(EmailOtp::getCode, code)
                .isNull(EmailOtp::getUsedAt) // 未被使用
                .ge(EmailOtp::getExpireAt, LocalDateTime.now()) // 未过期
                .orderByDesc(EmailOtp::getCreatedAt)
                .last("LIMIT 1"));

        if (otp == null) {
            throw new CustomException("验证码错误或已过期");
        }

        // 验证成功，标记为已使用
        otp.setUsedAt(LocalDateTime.now());
        this.updateById(otp);
    }

    private String getActionName(String scene) {
        switch (scene) {
            case "REGISTER": return "账号注册";
            case "LOGIN": return "验证码登录";
            case "RESET": return "修改密码";
            case "CANCEL": return "注销账号";
            case "RECOVER": return "恢复账号"; // [新增]
            default: return "身份验证";
        }
    }
}