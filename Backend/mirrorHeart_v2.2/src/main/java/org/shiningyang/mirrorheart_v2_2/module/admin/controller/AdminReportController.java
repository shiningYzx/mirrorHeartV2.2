package org.shiningyang.mirrorheart_v2_2.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.annotation.AuditLog;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.module.system.entity.Report;
import org.shiningyang.mirrorheart_v2_2.module.system.mapper.ReportMapper;
// 注意：如果您的 ReportHandleDto 在其他包，请修改此处的 import 路径
import org.shiningyang.mirrorheart_v2_2.module.system.dto.ReportHandleDto; 
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端：举报审核与处理 API
 */
@RestController
@RequestMapping("/api/v1/admin/report")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportMapper reportMapper;

    // ================= 管理端接口 =================

    /**
     * 管理员获取举报列表
     * @param status 可选：0=待处理, 1=已处理, 2=已忽略
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')") // 仅限管理员访问
    public Result<IPage<Report>> getReportList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Byte status) {

        Page<Report> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();

        // 如果传了状态，则按状态过滤
        wrapper.eq(status != null, Report::getStatus, status)
                .orderByDesc(Report::getCreatedAt); // 按时间倒序

        return Result.success(reportMapper.selectPage(page, wrapper));
    }

    /**
     * 管理员处理举报
     */
    @PutMapping("/handle")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "安全与治理", operation = "处理违规举报")
    public Result<String> handleReport(@RequestBody @Valid ReportHandleDto dto) {
        Report report = reportMapper.selectById(dto.getReportId());
        if (report == null) {
            throw new CustomException("举报记录不存在");
        }
        
        // 更新状态和批注
        report.setStatus(dto.getStatus());
        report.setAdminNote(dto.getAdminNote() == null ? "" : dto.getAdminNote());
        reportMapper.updateById(report);
        
        return Result.success("处理成功");
    }
}