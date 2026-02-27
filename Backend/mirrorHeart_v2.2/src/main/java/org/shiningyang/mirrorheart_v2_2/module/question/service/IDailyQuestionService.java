package org.shiningyang.mirrorheart_v2_2.module.question.service;

import org.shiningyang.mirrorheart_v2_2.module.question.dto.AnswerSubmitDto;
import org.shiningyang.mirrorheart_v2_2.module.question.dto.QuestionCreateDto;
import org.shiningyang.mirrorheart_v2_2.module.question.dto.TodayQuestionVo;
import org.shiningyang.mirrorheart_v2_2.module.question.entity.DailyQuestion;
import com.baomidou.mybatisplus.extension.service.IService;
import org.shiningyang.mirrorheart_v2_2.module.question.entity.UserDailyRecord;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;

/**
 * <p>
 * 问题库(不绑定日期) 服务类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
public interface IDailyQuestionService extends IService<DailyQuestion> {
    void addQuestion(QuestionCreateDto dto);
    TodayQuestionVo getOrGenerateTodayQuestion(Long userId);
    void submitAnswer(Long userId, AnswerSubmitDto dto);
}
