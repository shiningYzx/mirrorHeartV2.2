package org.shiningyang.mirrorheart_v2_2.module.auth.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.AccountDeletionRequest;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.shiningyang.mirrorheart_v2_2.module.auth.mapper.AccountDeletionRequestMapper;
import org.shiningyang.mirrorheart_v2_2.module.auth.mapper.UserMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 账号注销到期执行任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountDeletionTask {

    private final AccountDeletionRequestMapper requestMapper;
    private final UserMapper userMapper;

    /**
     * 每天凌晨 2:00 扫描并执行到期的注销请求
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void executeAccountDeletion() {
        log.info("== 开始扫描到期的注销账号请求 ==");

        // 查找：时间已到，且未被取消、未被执行的记录
        List<AccountDeletionRequest> dueRequests = requestMapper.selectList(
                new LambdaQueryWrapper<AccountDeletionRequest>()
                        .le(AccountDeletionRequest::getExecuteAfter, LocalDateTime.now())
                        .isNull(AccountDeletionRequest::getCanceledAt)
                        .isNull(AccountDeletionRequest::getExecutedAt)
        );

        int count = 0;
        for (AccountDeletionRequest request : dueRequests) {
            // 1. 将用户的 is_deleted 标记为 1 (彻底逻辑删除)
            User user = userMapper.selectById(request.getUserId());
            if (user != null) {
                user.setIsDeleted((byte) 1);
                user.setStatus((byte) 0);
                userMapper.updateById(user);
            }

            // 2. 标记请求已执行
            request.setExecutedAt(LocalDateTime.now());
            requestMapper.updateById(request);
            count++;
            
            log.info("执行注销用户: ID={}", request.getUserId());
        }

        log.info("== 扫描结束，共注销 {} 个账号 ==", count);
    }
}