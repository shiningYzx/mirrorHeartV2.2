package org.shiningyang.mirrorheart_v2_2.module.admin.service.impl;

import org.shiningyang.mirrorheart_v2_2.module.admin.entity.AdminPermission;
import org.shiningyang.mirrorheart_v2_2.module.admin.mapper.AdminPermissionMapper;
import org.shiningyang.mirrorheart_v2_2.module.admin.service.IAdminPermissionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * RBAC权限表 服务实现类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Service
public class AdminPermissionServiceImpl extends ServiceImpl<AdminPermissionMapper, AdminPermission> implements IAdminPermissionService {

}
