package org.shiningyang.mirrorheart_v2_2.module.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenVo {
    private String token;
    private String refreshToken; // [新增] 长效刷新令牌
    private String tokenHead;
    private Long expiresIn;
}