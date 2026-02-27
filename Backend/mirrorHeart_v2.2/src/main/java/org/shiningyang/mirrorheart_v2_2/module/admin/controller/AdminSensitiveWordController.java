package org.shiningyang.mirrorheart_v2_2.module.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.annotation.AuditLog;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.module.system.entity.SensitiveWord;
import org.shiningyang.mirrorheart_v2_2.module.system.service.SensitiveWordService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端：敏感词治理 API
 * 统一放在 admin 包下，路径以 /api/v1/admin 开头
 */
@RestController
@RequestMapping("/api/v1/admin/sensitive-word")
@RequiredArgsConstructor
public class AdminSensitiveWordController {

    private final SensitiveWordService sensitiveWordService;

    /**
     * 获取敏感词分页列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')") // 严格限制仅管理员可用
    public Result<IPage<SensitiveWord>> getList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        
        Page<SensitiveWord> page = new Page<>(pageNo, pageSize);
        return Result.success(sensitiveWordService.getWordPage(page, keyword));
    }

    /**
     * 新增敏感词
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "安全与治理", operation = "添加敏感词") // 记录审计日志
    public Result<String> addWord(@RequestBody Map<String, String> body) {
        String word = body.get("word");
        sensitiveWordService.addWord(word);
        return Result.success("添加成功，词库已实时刷新");
    }

    /**
     * 删除敏感词
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "安全与治理", operation = "删除敏感词")
    public Result<String> deleteWord(@PathVariable Long id) {
        sensitiveWordService.deleteWord(id);
        return Result.success("删除成功，词库已实时刷新");
    }
}