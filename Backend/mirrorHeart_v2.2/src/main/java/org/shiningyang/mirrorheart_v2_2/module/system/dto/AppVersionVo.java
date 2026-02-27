package org.shiningyang.mirrorheart_v2_2.module.system.dto;

import lombok.Data;

@Data
public class AppVersionVo {
    // 是否有新版本
    private Boolean hasUpdate;
    
    // 新版本信息 (当 hasUpdate 为 true 时有值)
    private String versionName;
    private Integer versionCode;
    private Boolean isForce; // 是否强制更新
    private String downloadUrl;
    private String updateLog;
}