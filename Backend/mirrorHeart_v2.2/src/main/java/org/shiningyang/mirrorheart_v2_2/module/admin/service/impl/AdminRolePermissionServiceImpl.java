package org.shiningyang.mirrorheart_v2_2.module.admin.service.impl;

import org.shiningyang.mirrorheart_v2_2.module.admin.entity.AdminRolePermission;
import org.shiningyang.mirrorheart_v2_2.module.admin.mapper.AdminRolePermissionMapper;
import org.shiningyang.mirrorheart_v2_2.module.admin.service.IAdminRolePermissionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 角色-权限关联表 服务实现类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Service
public class AdminRolePermissionServiceImpl extends ServiceImpl<AdminRolePermissionMapper, AdminRolePermission> implements IAdminRolePermissionService {

}
