package org.shiningyang.mirrorheart_v2_2.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.annotation.AuditLog;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.system.dto.ReportCreateDto;
import org.shiningyang.mirrorheart_v2_2.module.system.dto.ReportHandleDto;
import org.shiningyang.mirrorheart_v2_2.module.system.entity.Report;
import org.shiningyang.mirrorheart_v2_2.module.system.mapper.ReportMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/system/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportMapper reportMapper;

    @PostMapping("/submit")
    public Result<String> submitReport(@RequestBody @Valid ReportCreateDto dto) {
        Long userId = SecurityUtils.getUserId();

        // 简单防刷校验：同一用户对同一目标只能举报一次
        boolean exists = reportMapper.exists(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, userId) // 修改为 getReporterId
                .eq(Report::getTargetType, dto.getTargetType())
                .eq(Report::getTargetId, dto.getTargetId()));

        if (exists) {
            throw new CustomException("您已举报过该内容，请耐心等待管理员审核");
        }

        Report report = new Report();
        report.setReporterId(userId); // 修改为 setReporterId
        report.setTargetType(dto.getTargetType());
        report.setTargetId(dto.getTargetId());
        report.setReason(dto.getReason());
        report.setStatus((byte) 0); // 0=待处理

        reportMapper.insert(report);

        return Result.success("举报提交成功，感谢您维护社区环境！");
    }

    @GetMapping("/my-list")
    public Result<IPage<Report>> getMyReports(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        // 1. 获取当前登录用户的 ID
        Long userId = SecurityUtils.getUserId();

        // 2. 构建分页参数
        Page<Report> page = new Page<>(pageNo, pageSize);

        // 3. 构建查询条件：仅查询自己的举报记录，并按时间倒序排列（最新的在前面）
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Report::getReporterId, userId)
                .orderByDesc(Report::getCreatedAt);

        // 4. 执行查询
        IPage<Report> reportPage = reportMapper.selectPage(page, wrapper);

        return Result.success(reportPage);
    }
}