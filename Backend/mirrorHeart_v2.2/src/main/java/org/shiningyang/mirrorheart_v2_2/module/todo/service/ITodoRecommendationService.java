package org.shiningyang.mirrorheart_v2_2.module.todo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.shiningyang.mirrorheart_v2_2.module.todo.entity.TodoRecommendation;

import java.util.List;

public interface ITodoRecommendationService extends IService<TodoRecommendation> {
    
    /**
     * 随机获取指定数量的启用状态的推荐待办
     * @param count 需要获取的数量
     * @return 待办文案列表
     */
    List<String> getRandomRecommendations(int count);
}