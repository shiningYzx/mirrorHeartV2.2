package org.shiningyang.mirrorheart_v2_2.module.auth.service;

import cn.hutool.core.lang.UUID;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.common.result.ErrorCode;
import org.shiningyang.mirrorheart_v2_2.common.utils.JwtUtils;
import org.shiningyang.mirrorheart_v2_2.common.security.LoginUser;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.AuthActionDtos.*;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.LoginDto;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.RegisterDto;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.TokenVo;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.AccountDeletionRequest;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.AuthSession;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.shiningyang.mirrorheart_v2_2.module.auth.mapper.AccountDeletionRequestMapper;
import org.shiningyang.mirrorheart_v2_2.module.auth.mapper.AuthSessionMapper;
import org.shiningyang.mirrorheart_v2_2.module.auth.mapper.UserMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionMapper authSessionMapper;
    private final AuthenticationManager authenticationManager;
    private final AccountDeletionRequestMapper deletionRequestMapper; // [新增]
    private final JwtUtils jwtUtils;
    private final IEmailOtpService emailOtpService;
    private final UserDetailsService userDetailsService; // 用于免密登录加载权限

    /**
     * 发送验证码
     */
    public void sendEmailCode(SendOtpDto dto) {
        // 部分场景需要校验用户是否存在
        boolean userExists = userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
        if ("REGISTER".equals(dto.getScene()) && userExists) {
            throw new CustomException("该邮箱已被注册");
        }
        if (("LOGIN".equals(dto.getScene()) || "RESET".equals(dto.getScene()) ||
                "CANCEL".equals(dto.getScene()) || "RECOVER".equals(dto.getScene())) && !userExists) {
            throw new CustomException("该邮箱未注册");
        }

        emailOtpService.sendOtp(dto.getEmail(), dto.getScene());
    }

    /**
     * 用户注册 (需校验验证码)
     */
    public void register(RegisterDto dto) {
        // 1. 校验验证码
        emailOtpService.verifyOtp(dto.getEmail(), "REGISTER", dto.getCode());

        // 2. 检查邮箱是否重复
        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()))) {
            throw new CustomException(ErrorCode.DUPLICATE_KEY.getCode(), "该邮箱已被注册");
        }

        // 3. 构建并插入用户
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setNickname(dto.getNickname());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole((byte) 0);
        user.setStatus((byte) 1); 
        userMapper.insert(user);
    }

    /**
     * 密码登录
     */
    public TokenVo login(LoginDto dto) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword()));
        } catch (Exception e) {
            throw new CustomException(ErrorCode.PASSWORD_ERROR);
        }

        return generateTokenAfterAuth(authentication);
    }

    // 如果你的项目里用了 Redis 存 Token，请确保注入了 StringRedisTemplate
    private final StringRedisTemplate stringRedisTemplate;

    public void logout() {
        // 1. 获取当前登录用户的 ID
        Long userId = SecurityUtils.getUserId();

        // 2. 清理 Redis 中的 Token 缓存（如果你的 JWT 是纯无状态的，这一步可以省略）
        // 注意：这里的 Key 名字 ("login_token:" / "refresh_token:") 请替换成你项目里实际使用的 Key 名字！
        String tokenKey = "login_token:" + userId;
        String refreshTokenKey = "refresh_token:" + userId;

        stringRedisTemplate.delete(tokenKey);
        stringRedisTemplate.delete(refreshTokenKey);

        // 3. （可选）如果你有用户行为日志表，可以在这里记录一条“用户主动登出”的日志
        // logService.saveLog(userId, "退出登录");
    }

    /**
     * 验证码登录
     */
    public TokenVo loginByCode(EmailLoginDto dto) {
        // 1. 校验验证码
        emailOtpService.verifyOtp(dto.getEmail(), "LOGIN", dto.getCode());

        // 2. 确认用户存在
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
        if (user == null || user.getStatus() == 0) {
            throw new CustomException("用户不存在或已被禁用");
        }

        // 3. 绕过密码验证，手动加载用户详情并注入 Security 上下文
        UserDetails userDetails = userDetailsService.loadUserByUsername(dto.getEmail());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        
        return generateTokenAfterAuth(authentication);
    }

    /**
     * 修改/重置密码
     */
    public void resetPassword(ResetPwdDto dto) {
        emailOtpService.verifyOtp(dto.getEmail(), "RESET", dto.getCode());

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
        if (user == null) {
            throw new CustomException("用户不存在");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    /**
     * [重构] 申请注销账号 (进入30天冷静期)
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelAccount(CancelAccountDto dto) {
        emailOtpService.verifyOtp(dto.getEmail(), "CANCEL", dto.getCode());

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
        if (user == null) throw new CustomException("用户不存在");
        if (user.getStatus() == 2) throw new CustomException("该账号已处于注销冷静期");
        if (user.getIsDeleted() == 1) throw new CustomException("账号已被删除");

        // 1. 修改用户状态为 2 (冷静期，此时无法登录。LoginUser中已配置 status!=1 则 isEnabled=false)
        user.setStatus((byte) 2);
        userMapper.updateById(user);

        // 2. 插入冷静期请求表
        AccountDeletionRequest request = new AccountDeletionRequest();
        request.setUserId(user.getId());
        request.setRequestedAt(LocalDateTime.now());
        request.setExecuteAfter(LocalDateTime.now().plusDays(30)); // 30天冷静期
        request.setReason(dto.getReason() != null ? dto.getReason() : "用户主动申请注销");
        deletionRequestMapper.insert(request);
    }

    /**
     * [新增] 找回冷静期内的账号
     */
    @Transactional(rollbackFor = Exception.class)
    public void recoverAccount(RecoverAccountDto dto) {
        emailOtpService.verifyOtp(dto.getEmail(), "RECOVER", dto.getCode());

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
        if (user == null) throw new CustomException("用户不存在");
        if (user.getStatus() != 2) throw new CustomException("该账号状态正常，无需找回");

        // 1. 恢复用户状态为正常
        user.setStatus((byte) 1);
        userMapper.updateById(user);

        // 2. 取消未执行的注销请求
        AccountDeletionRequest request = deletionRequestMapper.selectOne(
                new LambdaQueryWrapper<AccountDeletionRequest>()
                        .eq(AccountDeletionRequest::getUserId, user.getId())
                        .isNull(AccountDeletionRequest::getCanceledAt)
                        .isNull(AccountDeletionRequest::getExecutedAt)
                        .orderByDesc(AccountDeletionRequest::getRequestedAt)
                        .last("LIMIT 1")
        );
        if (request != null) {
            request.setCanceledAt(LocalDateTime.now());
            deletionRequestMapper.updateById(request);
        }
    }

    /**
     * 无感刷新 Token (双 Token 机制 + JTI 会话替换)
     */
    @Transactional(rollbackFor = Exception.class)
    public TokenVo refreshToken(String refreshToken) {
        // 1. [新增] 严格校验传入的必须是 REFRESH 类型的 Token
        String tokenType = jwtUtils.getTokenTypeFromToken(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED.getCode(), "非法的令牌类型，请使用 Refresh Token");
        }

        String email = jwtUtils.getUsernameFromToken(refreshToken);
        String jti = jwtUtils.getJtiFromToken(refreshToken);

        if (email == null || jti == null || !jwtUtils.validateToken(refreshToken, email)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED.getCode(), "Refresh Token 无效或已过期，请重新登录");
        }

        // 2. 校验会话表里有没有这个 JTI，且不能是被撤销的状态
        AuthSession oldSession = authSessionMapper.selectOne(new LambdaQueryWrapper<AuthSession>()
                .eq(AuthSession::getJti, jti));
        if (oldSession == null || oldSession.getRevokedAt() != null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED.getCode(), "该会话已被作废，请重新登录");
        }

        // 3. 校验用户最新状态
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null || user.getStatus() != 1) {
            throw new CustomException("用户不存在或状态异常");
        }

        // 4. 销毁旧的会话
        oldSession.setRevokedAt(LocalDateTime.now());
        authSessionMapper.updateById(oldSession);

        // 5. 生成新的会话和双 Token
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        return generateTokenAfterAuth(authentication);
    }

    // ... [保留 logout 方法] ...

    /**
     * 生成双 Token 并入库记录会话
     */
    private TokenVo generateTokenAfterAuth(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        Long userId = loginUser.getUser().getId();
        String email = loginUser.getUser().getEmail();

        // 1. 生成本次会话唯一的 JTI
        String jti = UUID.fastUUID().toString(true);

        // 2. [修改] 生成携带类型标识的双 Token
        String accessToken = jwtUtils.generateToken(userId, email, 7200L, jti, "ACCESS");
        String refreshToken = jwtUtils.generateToken(userId, email, 604800L, jti, "REFRESH");

        // 3. 获取客户端环境信息
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ip = getIpAddress(request);

        // 🌟【核心优化】使用 Hutool 智能解析长串的 UserAgent
        String uaString = request.getHeader("User-Agent");
        String deviceName = "未知设备";
        if (uaString != null && !uaString.isEmpty()) {
            UserAgent ua = UserAgentUtil.parse(uaString);
            // 结果示例："Windows 10 / Chrome" 或 "Android / WeChat"
            deviceName = ua.getOs().getName() + " / " + ua.getBrowser().getName();
        }


        // 存库 (直接存入 device 字段，抛弃原始的 user_agent 字段)
        AuthSession session = new AuthSession();
        session.setUserId(userId);
        session.setJti(jti);
        session.setIp(ip);
        session.setDevice(deviceName); // 存入解析后的直观名称
        session.setExpiredAt(LocalDateTime.now().plusSeconds(604800L));
        session.setCreatedAt(LocalDateTime.now());
        authSessionMapper.insert(session);

        return TokenVo.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenHead("Bearer")
                .expiresIn(7200L)
                .build();
    }

    /**
     * 获取客户端真实 IP 地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于通过多个反向代理的IP，第一个通常是真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "127.0.0.1";
    }

    /**
     * 解析请求设备类型
     */
    private String parseDevice(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "UNKNOWN";
        }
        String ua = userAgent.toUpperCase();
        if (ua.contains("ANDROID")) {
            return "ANDROID";
        } else if (ua.contains("IPHONE") || ua.contains("IPAD") || ua.contains("MAC")) {
            return "IOS/MAC";
        } else if (ua.contains("WINDOWS")) {
            return "WINDOWS";
        }
        return "WEB";
    }
}