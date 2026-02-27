package org.shiningyang.mirrorheart_v2_2.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.module.system.dto.AppVersionVo;
import org.shiningyang.mirrorheart_v2_2.module.system.entity.AppVersion;
import org.shiningyang.mirrorheart_v2_2.module.system.service.IAppVersionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/version")
@RequiredArgsConstructor
public class AppVersionController {

    private final IAppVersionService appVersionService;

    /**
     * 检查 App 版本更新
     * 这个接口通常不需要 Token 认证（需在 SecurityConfig 中配置放行，或者作为公开接口访问）
     * * @param platform 平台：ANDROID 或 IOS
     * @param versionCode 客户端当前版本号 (如 100)
     */
    @GetMapping("/check")
    public Result<AppVersionVo> checkUpdate(
            @RequestParam(defaultValue = "ANDROID") String platform,
            @RequestParam Integer versionCode) {

        // 走缓存获取最新版本记录
        AppVersion latestVersion = appVersionService.getLatestVersion(platform);

        AppVersionVo vo = new AppVersionVo();

        // 比较版本号
        if (latestVersion != null && latestVersion.getVersionCode() > versionCode) {
            vo.setHasUpdate(true);
            vo.setVersionName(latestVersion.getVersionName());
            vo.setVersionCode(latestVersion.getVersionCode());
            vo.setIsForce(latestVersion.getIsForce() == 1);
            vo.setDownloadUrl(latestVersion.getDownloadUrl());
            vo.setUpdateLog(latestVersion.getUpdateLog());
        } else {
            vo.setHasUpdate(false);
        }

        return Result.success(vo);
    }
}