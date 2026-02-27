package org.shiningyang.mirrorheart_v2_2.module.todo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.shiningyang.mirrorheart_v2_2.module.todo.entity.TodoRecommendation;
import org.shiningyang.mirrorheart_v2_2.module.todo.mapper.TodoRecommendationMapper;
import org.shiningyang.mirrorheart_v2_2.module.todo.service.ITodoRecommendationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TodoRecommendationServiceImpl extends ServiceImpl<TodoRecommendationMapper, TodoRecommendation> implements ITodoRecommendationService {

    @Override
    public List<String> getRandomRecommendations(int count) {
        // 利用 MySQL 的 ORDER BY RAND() 进行随机抽取
        // 由于推荐池数据量通常较小（几百条以内），此写法性能完全可以接受
        List<TodoRecommendation> list = this.list(new LambdaQueryWrapper<TodoRecommendation>()
                .eq(TodoRecommendation::getStatus, 1)
                .last("ORDER BY RAND() LIMIT " + count));

        // 仅提取文字内容返回给前端，简化前端解析逻辑
        return list.stream()
                .map(TodoRecommendation::getText)
                .collect(Collectors.toList());
    }
}