package org.shiningyang.mirrorheart_v2_2.module.recommend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.recommend.dto.DailyRecommendationVo;
import org.shiningyang.mirrorheart_v2_2.module.recommend.entity.DailyRecommendation;
import org.shiningyang.mirrorheart_v2_2.module.recommend.service.IDailyRecommendationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/recommend")
@RequiredArgsConstructor
public class DailyRecommendationController {

    private final IDailyRecommendationService recommendationService;

    @GetMapping("/today")
    public Result<DailyRecommendationVo> getTodayRecommendation() {
        Long userId = null;
        try {
            // 如果用户登录了，带了正确的 Token，就能拿到 ID
            userId = SecurityUtils.getUserId();
        } catch (Exception e) {
            // 🌟 捕获异常，如果没带 Token 或 Token 过期，就当作游客 (userId = null)
            // 此时什么都不用做，优雅降级
        }
        return Result.success(recommendationService.getOrGenerateTodayRecommendation(userId));
    }

    /**
     * [新增] 按日期查询历史推荐
     * 示例: /api/v1/recommend/date?date=2023-10-24
     */
    @GetMapping("/date")
    public Result<DailyRecommendationVo> getRecommendationByDate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Long userId = SecurityUtils.getUserId();
        DailyRecommendationVo vo = recommendationService.getRecommendationByDate(userId, date);

        Result<DailyRecommendationVo> result = Result.success(vo);
        if (vo == null) {
            // 区分处理：如果当日无数据，返回 null 并修改提示信息，方便前端展示空状态
            result.setMessage("该日期没有推荐记录");
        }

        return result;
    }

    /**
     * [新增] 分页获取历史推荐列表
     */
    @GetMapping("/history")
    public Result<IPage<DailyRecommendationVo>> getHistoryList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = SecurityUtils.getUserId();
        Page<DailyRecommendation> page = new Page<>(pageNo, pageSize);
        return Result.success(recommendationService.getHistoryList(page, userId));
    }
}