package org.shiningyang.mirrorheart_v2_2.module.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.annotation.AuditLog;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.module.question.dto.QuestionCreateDto;
import org.shiningyang.mirrorheart_v2_2.module.question.service.IDailyQuestionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端：每日一问题库管理 API
 */
@RestController
@RequestMapping("/api/v1/admin/question")
@RequiredArgsConstructor
public class AdminDailyQuestionController {

    private final IDailyQuestionService dailyQuestionService;

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "每日一问", operation = "添加题库题目")
    public Result<String> addQuestion(@RequestBody @Valid QuestionCreateDto dto) {
        dailyQuestionService.addQuestion(dto);
        return Result.success("题目添加成功");
    }
}