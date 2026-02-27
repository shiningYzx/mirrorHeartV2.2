package org.shiningyang.mirrorheart_v2_2.module.question.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserInfoVo;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.shiningyang.mirrorheart_v2_2.module.auth.mapper.UserMapper;
import org.shiningyang.mirrorheart_v2_2.module.auth.service.IUserService;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.LikeAction;
import org.shiningyang.mirrorheart_v2_2.module.interaction.mapper.LikeActionMapper;
import org.shiningyang.mirrorheart_v2_2.module.question.dto.UserDailyRecordVo;
import org.shiningyang.mirrorheart_v2_2.module.question.entity.UserDailyRecord;
import org.shiningyang.mirrorheart_v2_2.module.question.mapper.UserDailyRecordMapper;
import org.shiningyang.mirrorheart_v2_2.module.question.service.IUserDailyRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 每日一问记录(推送+回答) 服务实现类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class UserDailyRecordServiceImpl extends ServiceImpl<UserDailyRecordMapper, UserDailyRecord> implements IUserDailyRecordService {
    private final LikeActionMapper likeActionMapper; // 🌟 必须注入点赞Mapper
//    private final IUserService userService; // 用于组装回答者信息
    // 注入用于装配回答列表的 Mapper
    private final UserMapper userMapper;
    private final UserDailyRecordMapper userDailyRecordMapper;

    /**
     * 获取指定问题的回答列表 (按时间倒序)
     */
//    @Override
//    public IPage<UserDailyRecordVo> getAnswerList(Page<UserDailyRecord> pageParam, Long questionId, Long currentUserId) {
//        // 1. 查询该问题的公开回答
//        LambdaQueryWrapper<UserDailyRecord> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(UserDailyRecord::getQuestionId, questionId);
//        wrapper.eq(UserDailyRecord::getVisibility, 0); // 假设 0=公开可见
//        wrapper.orderByDesc(UserDailyRecord::getCreatedAt); // 最新回答排在前面
//
//        Page<UserDailyRecord> recordPage = this.page(pageParam, wrapper);
//
//        // 2. 转换为 VO
//        IPage<UserDailyRecordVo> voPage = recordPage.convert(record -> {
//            UserDailyRecordVo vo = new UserDailyRecordVo();
//            BeanUtils.copyProperties(record, vo);
//
//            // 装配回答者的信息
//            User author = userService.getById(record.getUserId());
//            if (author != null) {
//                vo.setAuthor(UserInfoVo.fromUser(author));
//            }
//            return vo;
//        });
//
//        // 🌟 3. 【核心批量装配点赞状态】
//        if (currentUserId != null && !voPage.getRecords().isEmpty()) {
//            List<Long> answerIds = voPage.getRecords().stream()
//                    .map(UserDailyRecordVo::getId)
//                    .collect(Collectors.toList());
//
//            Set<Long> likedAnswerIds = likeActionMapper.selectList(new LambdaQueryWrapper<LikeAction>()
//                    .eq(LikeAction::getUserId, currentUserId)
//                    .eq(LikeAction::getTargetType, "DAILY_ANSWER") // 目标类型对应每日一问回答
//                    .in(LikeAction::getTargetId, answerIds)
//            ).stream().map(LikeAction::getTargetId).collect(Collectors.toSet());
//
//            voPage.getRecords().forEach(vo -> {
//                vo.setIsLiked(likedAnswerIds.contains(vo.getId()));
//            });
//        }
//
//        return voPage;
//    }

    /**
     * 🌟 新增：分页获取某个问题的公开回答列表 (广场)
     */
    public IPage<UserDailyRecordVo> getAnswerList(Page<UserDailyRecord> page, Long questionId, Long currentUserId) {
        LambdaQueryWrapper<UserDailyRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDailyRecord::getQuestionId, questionId)
                .eq(UserDailyRecord::getVisibility, 0) // 0=公开
                // 只要文字不为空，或者录音不为空，就认为是有效回答
                .and(w -> w.isNotNull(UserDailyRecord::getAnswerText).ne(UserDailyRecord::getAnswerText, "")
                        .or()
                        .isNotNull(UserDailyRecord::getAudioUrl).ne(UserDailyRecord::getAudioUrl, ""));

        // 按热度及回答时间倒序
        wrapper.orderByDesc(UserDailyRecord::getHeatScore, UserDailyRecord::getAnsweredAt);

        Page<UserDailyRecord> recordPage = userDailyRecordMapper.selectPage(page, wrapper);

        // 转换为 VO 视图对象
        return recordPage.convert(record -> {
            UserDailyRecordVo vo = new UserDailyRecordVo();
            BeanUtils.copyProperties(record, vo);

            // 兼容旧字段映射：将 answerText 放到 content 里给前端展示
            vo.setContent(record.getAnswerText());

            // 1. 装配作者信息
            User author = userMapper.selectById(record.getUserId());
            if (author != null) {
                vo.setAuthor(UserInfoVo.fromUser(author));
            }

            // 2. 装配当前用户的点赞状态
            if (currentUserId != null) {
                boolean isLiked = likeActionMapper.exists(new LambdaQueryWrapper<LikeAction>()
                        .eq(LikeAction::getUserId, currentUserId)
                        .eq(LikeAction::getTargetType, "DAILY_ANSWER")
                        .eq(LikeAction::getTargetId, record.getId()));
                vo.setIsLiked(isLiked);
            }

            return vo;
        });
    }
}
