package org.shiningyang.mirrorheart_v2_2.module.todo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.system.service.SensitiveWordService;
import org.shiningyang.mirrorheart_v2_2.module.todo.dto.TodoCreateDto;
import org.shiningyang.mirrorheart_v2_2.module.todo.dto.TodoUpdateDto;
import org.shiningyang.mirrorheart_v2_2.module.todo.entity.TodoItem;
import org.shiningyang.mirrorheart_v2_2.module.todo.service.ITodoItemService;
import org.shiningyang.mirrorheart_v2_2.module.todo.service.ITodoRecommendationService;
import org.shiningyang.mirrorheart_v2_2.module.todo.task.TodoWeeklySummaryTask;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/todo")
@RequiredArgsConstructor
public class TodoController {

    private final ITodoItemService todoItemService;
    private final ITodoRecommendationService todoRecommendationService;
    private final SensitiveWordService sensitiveWordService;
    private final TodoWeeklySummaryTask todoWeeklySummaryTask; // 注入总结任务组件

    // ==========================================
    // 1. 微光行动 - 灵感推荐 (新增)
    // ==========================================

    /**
     * 获取随机推荐灵感 (每次请求返回不同的数据)
     * @param count 请求的数量，默认 3 条
     */
    @GetMapping("/recommend")
    public Result<List<String>> getRandomRecommendations(@RequestParam(defaultValue = "3") Integer count) {
        // 防止前端恶意请求过大数量导致数据库压力
        if (count > 10) count = 10;
        return Result.success(todoRecommendationService.getRandomRecommendations(count));
    }

    /**
     * 添加待办事项
     */
    @PostMapping("/add")
    public Result<String> addTodo(@RequestBody @Valid TodoCreateDto dto) {
        // 敏感词校验
        String match = sensitiveWordService.getFirstMatchWord(dto.getTitle());
        if (match != null) {
            throw new CustomException("待办事项包含违规词汇：" + match);
        }

        Long userId = SecurityUtils.getUserId();

        TodoItem item = new TodoItem();
        item.setUserId(userId);
        item.setTitle(dto.getTitle());
        item.setStatus((byte) 0); // 默认未完成

        todoItemService.save(item);
        return Result.success("添加成功");
    }

    /**
     * 获取待办事项列表
     * @param status 可选：传0查未完成，传1查已完成，不传查所有
     */
    @GetMapping("/list")
    public Result<List<TodoItem>> getTodoList(@RequestParam(required = false) Byte status) {
        Long userId = SecurityUtils.getUserId();

        List<TodoItem> list = todoItemService.list(new LambdaQueryWrapper<TodoItem>()
                .eq(TodoItem::getUserId, userId)
                .eq(status != null, TodoItem::getStatus, status)
                .orderByDesc(TodoItem::getCreatedAt)); // 新建的在前面

        return Result.success(list);
    }

    /**
     * 修改待办事项（重命名 或 标记完成/未完成）
     */
    @PutMapping("/{id}")
    public Result<String> updateTodo(@PathVariable Long id, @RequestBody TodoUpdateDto dto) {
        Long userId = SecurityUtils.getUserId();
        TodoItem item = todoItemService.getById(id);

        if (item == null || !item.getUserId().equals(userId)) {
            throw new CustomException("待办事项不存在或无权操作");
        }

        // 🌟 使用 UpdateWrapper 替代 updateById，解决 MyBatis-Plus 默认忽略 NULL 值更新的问题
        LambdaUpdateWrapper<TodoItem> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TodoItem::getId, id);

        // 处理标题更新
        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            String match = sensitiveWordService.getFirstMatchWord(dto.getTitle());
            if (match != null) {
                throw new CustomException("待办事项包含违规词汇：" + match);
            }
            updateWrapper.set(TodoItem::getTitle, dto.getTitle().trim());
        }

        // 🌟 处理状态与完成时间更新
        if (dto.getStatus() != null) {
            updateWrapper.set(TodoItem::getStatus, dto.getStatus());
            if (dto.getStatus() == 1) {
                // 如果标记为已完成，记录当前时间
                updateWrapper.set(TodoItem::getCompletedAt, LocalDateTime.now());
            } else {
                // 如果取消完成状态，强制将完成时间置为 NULL
                updateWrapper.set(TodoItem::getCompletedAt, null);
            }
        }

        todoItemService.update(updateWrapper);
        return Result.success("更新成功");
    }

    /**
     * 删除待办事项
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteTodo(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();

        TodoItem item = todoItemService.getById(id);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new CustomException("待办事项不存在或无权操作");
        }

        todoItemService.removeById(id);
        return Result.success("删除成功");
    }

    // ==========================================
    // 3. 🌟 新增：手动触发每周星空总结 (测试用)
    // ==========================================
    @PostMapping("/test-weekly-summary")
    public Result<String> triggerWeeklySummary() {
        // 使用新线程异步执行，防止大模型请求导致接口阻塞超时
        new Thread(todoWeeklySummaryTask::executeWeeklySummary).start();
        return Result.success("每周总结生成任务已在后台触发，稍后请查看系统通知");
    }
}