package org.shiningyang.mirrorheart_v2_2.module.system.service;

import org.shiningyang.mirrorheart_v2_2.module.system.entity.AppVersion;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * App版本管理 服务类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
public interface IAppVersionService extends IService<AppVersion> {
    /**
     * 获取指定平台的最新的 App 版本记录 (走 Redis 缓存)
     */
    AppVersion getLatestVersion(String platform);
}
