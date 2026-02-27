package org.shiningyang.mirrorheart_v2_2.module.question.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.question.dto.UserDailyRecordVo;
import org.shiningyang.mirrorheart_v2_2.module.question.entity.UserDailyRecord;
import org.shiningyang.mirrorheart_v2_2.module.question.service.IUserDailyRecordService;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 每日一问记录(推送+回答) 前端控制器
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/question/userDailyRecord")
public class UserDailyRecordController {
    // 注入 Service
    private final IUserDailyRecordService userDailyRecordService;

    /**
     * 分页获取某个【每日一问】的他人回答列表
     */
    @GetMapping("/{questionId}/answers")
    public Result<IPage<UserDailyRecordVo>> getQuestionAnswers(
            @PathVariable Long questionId,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        // 获取当前登录用户 ID (用于判断是否点过赞)
        Long currentUserId = null;
        try {
            currentUserId = SecurityUtils.getUserId();
        } catch (Exception e) {
            // 捕获异常，代表用户未登录，不阻断正常查看列表，只是 isLiked 全是 false
        }

        Page<UserDailyRecord> pageParam = new Page<>(pageNo, pageSize);
        IPage<UserDailyRecordVo> result = userDailyRecordService.getAnswerList(pageParam, questionId, currentUserId);

        return Result.success(result);
    }
}
