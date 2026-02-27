package org.shiningyang.mirrorheart_v2_2.module.question.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.module.question.dto.AnswerSubmitDto;
import org.shiningyang.mirrorheart_v2_2.module.question.dto.QuestionCreateDto;
import org.shiningyang.mirrorheart_v2_2.module.question.dto.TodayQuestionVo;
import org.shiningyang.mirrorheart_v2_2.module.question.entity.DailyQuestion;
import org.shiningyang.mirrorheart_v2_2.module.question.entity.UserDailyRecord;
import org.shiningyang.mirrorheart_v2_2.module.question.mapper.DailyQuestionMapper;
import org.shiningyang.mirrorheart_v2_2.module.question.mapper.UserDailyRecordMapper;
import org.shiningyang.mirrorheart_v2_2.module.question.service.IDailyQuestionService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyQuestionServiceImpl extends ServiceImpl<DailyQuestionMapper, DailyQuestion> implements IDailyQuestionService {

    private final UserDailyRecordMapper userDailyRecordMapper;


    /**
     * 管理员添加题目
     */
    @Override
    public void addQuestion(QuestionCreateDto dto) {
        // 1. 去除首尾空格，防止因为多敲了一个空格导致防重失效
        String cleanContent = dto.getText().trim();

        // 🌟【核心防重逻辑】：检查题库中是否已经存在一模一样的问题
        boolean exists = this.baseMapper.exists(new LambdaQueryWrapper<DailyQuestion>()
                .eq(DailyQuestion::getText, cleanContent));
        if (exists) {
            throw new CustomException("该问题已存在题库中，请勿重复添加！");
        }

        DailyQuestion q = new DailyQuestion();
        BeanUtils.copyProperties(dto, q);
        q.setUseCount(0);
        q.setStatus((byte) 1); // 1=启用
        this.save(q);
    }

    /**
     * 获取(或生成)今日问题
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodayQuestionVo getOrGenerateTodayQuestion(Long userId) {
        LocalDate today = LocalDate.now();

        // 【核心】：如果 userId 为 null，我们将查询目标指向虚拟游客 ID: 0L
        userId = (userId == null) ? 0L : userId;

        // 1. 先查 user_daily_record 表，看今天是否已经推过了
        UserDailyRecord record = userDailyRecordMapper.selectOne(new LambdaQueryWrapper<UserDailyRecord>()
                .eq(UserDailyRecord::getUserId, userId)
                .eq(UserDailyRecord::getDay, today));

        // 2. 如果没推过，就从题库里捞一道
        if (record == null) {
            record = pushNewQuestion(userId, today);
        }

        // 3. 组装返回数据
        DailyQuestion question = this.getById(record.getQuestionId());

        return getTodayQuestionVo(record, question);
    }

    @Nonnull
    private TodayQuestionVo getTodayQuestionVo(UserDailyRecord record, DailyQuestion question) {
        TodayQuestionVo vo = new TodayQuestionVo();
        vo.setRecordId(record.getId());
        vo.setQuestionId(question.getId());
        vo.setText(question.getText());
        vo.setTopic(question.getTopic());
        vo.setDay(record.getDay());


        // 🌟 修改：判断是否已回答 (audioUrl 或 answerText 任一不为空即为已回答)
        boolean hasAudio = record.getAudioUrl() != null && !record.getAudioUrl().trim().isEmpty();
        boolean hasText = record.getAnswerText() != null && !record.getAnswerText().trim().isEmpty();

        // 判断是否已回答 (audioUrl 不为空即为已回答)
        if (hasAudio && hasText) {
            vo.setAnswered(true);
            vo.setMyAudioUrl(record.getAudioUrl());
            vo.setMyAnswerText(record.getAnswerText()); // 注入文本
            vo.setAnsweredAt(record.getAnsweredAt());
        } else {
            vo.setAnswered(false);
        }
        return vo;
    }

    /**
     * 内部方法：从题库随机选一题推给用户
     * (简单实现：随机选一个启用的题目。进阶实现可过滤已答过的)
     */
    private UserDailyRecord pushNewQuestion(Long userId, LocalDate today) {
        // 这里简单处理：直接查所有启用题目，随机取一个
        // 生产环境建议用 SQL: ORDER BY RAND() LIMIT 1，或者 Redis Set 随机 pop
        List<DailyQuestion> questions = this.list(new LambdaQueryWrapper<DailyQuestion>()
                .eq(DailyQuestion::getStatus, 1));
        
        if (questions.isEmpty()) {
            throw new CustomException("题库为空，请联系管理员添加题目");
        }
        
        // 随机选一个
        int randomIndex = (int) (Math.random() * questions.size());
        DailyQuestion selectedQ = questions.get(randomIndex);

        // 创建推送记录
        UserDailyRecord newRecord = new UserDailyRecord();
        newRecord.setUserId(userId);
        newRecord.setDay(today);
        newRecord.setQuestionId(selectedQ.getId());
        newRecord.setLikeCount(0);
        newRecord.setHeatScore(0L);
        newRecord.setVisibility((byte) 0); // 默认公开
        
        userDailyRecordMapper.insert(newRecord);
        
        // 更新题目被使用次数
        selectedQ.setUseCount(selectedQ.getUseCount() + 1);
        this.updateById(selectedQ);
        
        return newRecord;
    }

    /**
     * 提交回答
     */
    public void submitAnswer(Long userId, AnswerSubmitDto dto) {
        // 🌟 新增：业务拦截，不能发全空的卡片
        boolean isAudioEmpty = dto.getAudioUrl() == null || dto.getAudioUrl().trim().isEmpty();
        boolean isTextEmpty = dto.getText() == null || dto.getText().trim().isEmpty();
        if (isAudioEmpty && isTextEmpty) {
            throw new CustomException("请至少写点什么，或者说点什么吧~");
        }

        LocalDate today = LocalDate.now();
        
        // 1. 查当天的推送记录
        UserDailyRecord record = userDailyRecordMapper.selectOne(new LambdaQueryWrapper<UserDailyRecord>()
                .eq(UserDailyRecord::getUserId, userId)
                .eq(UserDailyRecord::getDay, today)
                .eq(UserDailyRecord::getQuestionId, dto.getQuestionId())); // 双重校验
        
        if (record == null) {
            throw new CustomException("未找到今日该问题的推送记录");
        }
        
        // 2. 更新回答信息
        record.setAudioUrl(dto.getAudioUrl());
        record.setDurationMs(dto.getDurationMs());
        record.setAnswerText(dto.getText()); // 🌟 新增：保存文本内容
        record.setVisibility(dto.getVisibility().byteValue());
        record.setAnsweredAt(LocalDateTime.now());
        
        userDailyRecordMapper.updateById(record);
    }
}