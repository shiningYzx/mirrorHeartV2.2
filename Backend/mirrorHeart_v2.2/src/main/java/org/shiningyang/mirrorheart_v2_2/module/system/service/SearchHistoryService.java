package org.shiningyang.mirrorheart_v2_2.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.shiningyang.mirrorheart_v2_2.module.system.entity.SearchHistory;
import org.shiningyang.mirrorheart_v2_2.module.system.mapper.SearchHistoryMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchHistoryService extends ServiceImpl<SearchHistoryMapper, SearchHistory> {

    /**
     * 异步记录搜索历史
     * 使用 @Async 不阻塞用户的实际搜索请求
     */
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void addHistoryAsync(Long userId, String keyword) {
        if (userId == null || keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        
        String cleanKeyword = keyword.trim();

        // 1. 查询是否已经搜索过这个词
        SearchHistory existHistory = this.getOne(new LambdaQueryWrapper<SearchHistory>()
                .eq(SearchHistory::getUserId, userId)
                .eq(SearchHistory::getKeyword, cleanKeyword)
                .last("LIMIT 1"));

        if (existHistory != null) {
            // 如果搜过，则更新时间，让它重新排到第一位
            existHistory.setCreatedAt(LocalDateTime.now());
            this.updateById(existHistory);
        } else {
            // 如果没搜过，新增记录
            SearchHistory history = new SearchHistory();
            history.setUserId(userId);
            history.setKeyword(cleanKeyword);
            this.save(history);
        }

        // 2. 数量限制：每个用户最多保留 20 条搜索历史，超出则删除最老的数据
        long count = this.count(new LambdaQueryWrapper<SearchHistory>().eq(SearchHistory::getUserId, userId));
        if (count > 20) {
            // 找出超过 20 条以外的旧数据 ID
            List<Long> oldIds = this.list(new LambdaQueryWrapper<SearchHistory>()
                    .eq(SearchHistory::getUserId, userId)
                    .orderByDesc(SearchHistory::getCreatedAt))
                    .stream()
                    .skip(20) // 跳过前 20 个最新的
                    .map(SearchHistory::getId)
                    .collect(Collectors.toList());

            if (!oldIds.isEmpty()) {
                this.removeByIds(oldIds);
            }
        }
    }

    /**
     * 获取我的搜索历史 (按时间倒序)
     */
    public List<String> getMyHistory(Long userId) {
        return this.list(new LambdaQueryWrapper<SearchHistory>()
                .eq(SearchHistory::getUserId, userId)
                .orderByDesc(SearchHistory::getCreatedAt)
                .last("LIMIT 20"))
                .stream()
                .map(SearchHistory::getKeyword)
                .collect(Collectors.toList());
    }

    /**
     * 清空我的搜索历史
     */
    public void clearMyHistory(Long userId) {
        this.remove(new LambdaQueryWrapper<SearchHistory>().eq(SearchHistory::getUserId, userId));
    }
}