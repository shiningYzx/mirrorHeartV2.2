package org.shiningyang.mirrorheart_v2_2.module.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public  class ReportHandleDto {
    @NotNull(message = "举报ID不能为空")
    private Long reportId;

    @NotNull(message = "处理状态不能为空(1=已处理, 2=忽略)")
    private Byte status;

    private String adminNote; // 管理员处理批注
}
