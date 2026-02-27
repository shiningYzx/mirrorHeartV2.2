package org.shiningyang.mirrorheart_v2_2.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.shiningyang.mirrorheart_v2_2.module.system.entity.AppVersion;
import org.shiningyang.mirrorheart_v2_2.module.system.mapper.AppVersionMapper;
import org.shiningyang.mirrorheart_v2_2.module.system.service.IAppVersionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * <p>
 * App版本管理 服务实现类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Service
public class AppVersionServiceImpl extends ServiceImpl<AppVersionMapper, AppVersion> implements IAppVersionService {
    @Override
    @Cacheable(value = "app:version:latest", key = "#platform")
    public AppVersion getLatestVersion(String platform) {
        return this.getOne(new LambdaQueryWrapper<AppVersion>()
                .eq(AppVersion::getPlatform, platform.toUpperCase())
                .orderByDesc(AppVersion::getVersionCode)
                .last("LIMIT 1"));
    }
}
