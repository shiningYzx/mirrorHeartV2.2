package org.shiningyang.mirrorheart_v2_2.module.auth.service.impl;

import org.shiningyang.mirrorheart_v2_2.module.auth.entity.AuthSession;
import org.shiningyang.mirrorheart_v2_2.module.auth.mapper.AuthSessionMapper;
import org.shiningyang.mirrorheart_v2_2.module.auth.service.IAuthSessionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 认证会话表 服务实现类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Service
public class AuthSessionServiceImpl extends ServiceImpl<AuthSessionMapper, AuthSession> implements IAuthSessionService {

}
