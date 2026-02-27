package org.shiningyang.mirrorheart_v2_2.module.admin.service.impl;

import org.shiningyang.mirrorheart_v2_2.module.admin.entity.AdminAuditLog;
import org.shiningyang.mirrorheart_v2_2.module.admin.mapper.AdminAuditLogMapper;
import org.shiningyang.mirrorheart_v2_2.module.admin.service.IAdminAuditLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 管理端审计日志 服务实现类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Service
public class AdminAuditLogServiceImpl extends ServiceImpl<AdminAuditLogMapper, AdminAuditLog> implements IAdminAuditLogService {

}
