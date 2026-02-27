package org.shiningyang.mirrorheart_v2_2.module.question.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TodayQuestionVo {
    private Long recordId;      // 推送记录ID
    private Long questionId;    // 题目ID
    private String text;        // 题目内容
    private String topic;       // 话题
    private LocalDate day;      // 日期
    
    // 回答状态
    private boolean isAnswered; // 是否已回答
    private String myAudioUrl;  // 我的回答(如果有)
    private String myAnswerText; // 🌟 新增：我的文本回答(如果有)
    private LocalDateTime answeredAt; // 回答时间
}