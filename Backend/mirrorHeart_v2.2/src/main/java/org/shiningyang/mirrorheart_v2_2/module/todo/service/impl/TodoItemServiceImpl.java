package org.shiningyang.mirrorheart_v2_2.module.todo.service.impl;

import org.shiningyang.mirrorheart_v2_2.module.todo.entity.TodoItem;
import org.shiningyang.mirrorheart_v2_2.module.todo.mapper.TodoItemMapper;
import org.shiningyang.mirrorheart_v2_2.module.todo.service.ITodoItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户待办事项 服务实现类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
@Service
public class TodoItemServiceImpl extends ServiceImpl<TodoItemMapper, TodoItem> implements ITodoItemService {

}
