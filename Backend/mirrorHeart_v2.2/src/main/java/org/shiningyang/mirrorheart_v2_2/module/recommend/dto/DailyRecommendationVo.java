package org.shiningyang.mirrorheart_v2_2.module.recommend.dto;

import lombok.Data;
import org.shiningyang.mirrorheart_v2_2.module.content.dto.ContentDetailVo;

import java.time.LocalDate;
import java.util.List;

@Data
public class DailyRecommendationVo {
    private Long id;            // 推荐记录ID
    private LocalDate day;      // 日期
    private String strategy;    // 推荐策略
    private List<ContentDetailVo> items; // 具体的推荐内容列表
}