package org.shiningyang.mirrorheart_v2_2.module.question.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.question.dto.AnswerSubmitDto;
import org.shiningyang.mirrorheart_v2_2.module.question.dto.TodayQuestionVo;
import org.shiningyang.mirrorheart_v2_2.module.question.service.IDailyQuestionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/question")
@RequiredArgsConstructor
public class DailyQuestionController {
    private final IDailyQuestionService dailyQuestionService;

    // --- 用户端接口 ---

    @GetMapping("/today")
    public Result<TodayQuestionVo> getTodayQuestion() {
        // 🌟 必须初始化为 null
        Long userId = null;
        try {
            // 尝试获取登录用户的 ID
            userId = SecurityUtils.getUserId();
        } catch (Exception e) {
            // 🌟 捕获异常：如果是游客没带 Token，程序不会报错崩溃，而是让 currentUserId 保持为 null
        }

        return Result.success(dailyQuestionService.getOrGenerateTodayQuestion(userId));
    }

    @PostMapping("/answer")
    public Result<String> submitAnswer(@RequestBody @Valid AnswerSubmitDto dto) {
        Long userId = SecurityUtils.getUserId();
        dailyQuestionService.submitAnswer(userId, dto);
        return Result.success("回答已提交");
    }
}