package org.shiningyang.mirrorheart_v2_2.module.recommend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.shiningyang.mirrorheart_v2_2.module.recommend.dto.DailyRecommendationVo;
import org.shiningyang.mirrorheart_v2_2.module.recommend.entity.DailyRecommendation;

import java.time.LocalDate;

public interface IDailyRecommendationService extends IService<DailyRecommendation> {
    /**
     * 获取(或生成)今日推荐
     */
    DailyRecommendationVo getOrGenerateTodayRecommendation(Long userId);

    /**
     * [新增] 按日期查询推荐 (回溯)
     */
    DailyRecommendationVo getRecommendationByDate(Long userId, LocalDate date);

    /**
     * [新增] 分页查询历史推荐列表
     */
    IPage<DailyRecommendationVo> getHistoryList(Page<DailyRecommendation> page, Long userId);
}