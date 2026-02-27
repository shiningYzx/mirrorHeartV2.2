package org.shiningyang.mirrorheart_v2_2.module.question.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.shiningyang.mirrorheart_v2_2.module.question.dto.UserDailyRecordVo;
import org.shiningyang.mirrorheart_v2_2.module.question.entity.UserDailyRecord;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 每日一问记录(推送+回答) 服务类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
public interface IUserDailyRecordService extends IService<UserDailyRecord> {
    IPage<UserDailyRecordVo> getAnswerList(Page<UserDailyRecord> pageParam, Long questionId, Long currentUserId);
}
