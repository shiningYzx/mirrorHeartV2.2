package org.shiningyang.mirrorheart_v2_2.module.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.annotation.AuditLog;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.module.todo.entity.TodoRecommendation;
import org.shiningyang.mirrorheart_v2_2.module.todo.service.ITodoRecommendationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端：微光行动灵感池管理 API
 */
@RestController
@RequestMapping("/api/v1/admin/todo/recommend")
@RequiredArgsConstructor
public class AdminTodoController {

    private final ITodoRecommendationService recommendationService;

    /**
     * 获取推荐池列表 (分页)
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<TodoRecommendation>> getList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
            
        Page<TodoRecommendation> page = new Page<>(pageNo, pageSize);
        return Result.success(recommendationService.page(page));
    }

    /**
     * 向推荐池中添加新灵感
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "微光行动管理", operation = "添加推荐灵感")
    public Result<String> addRecommendation(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.trim().isEmpty()) {
            throw new CustomException("推荐内容不能为空");
        }
        
        TodoRecommendation rec = new TodoRecommendation();
        rec.setText(text.trim());
        rec.setStatus((byte) 1); // 默认启用
        recommendationService.save(rec);
        
        return Result.success("添加成功");
    }

    /**
     * 修改推荐项的状态 (下架/重新上架)
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "微光行动管理", operation = "修改推荐状态")
    public Result<String> updateStatus(@PathVariable Long id, @RequestParam Byte status) {
        TodoRecommendation rec = recommendationService.getById(id);
        if (rec == null) {
            throw new CustomException("记录不存在");
        }
        rec.setStatus(status);
        recommendationService.updateById(rec);
        return Result.success(status == 1 ? "已上架展示" : "已下架隐藏");
    }

    /**
     * 删除推荐项
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "微光行动管理", operation = "删除推荐灵感")
    public Result<String> deleteRecommendation(@PathVariable Long id) {
        recommendationService.removeById(id);
        return Result.success("删除成功");
    }
}