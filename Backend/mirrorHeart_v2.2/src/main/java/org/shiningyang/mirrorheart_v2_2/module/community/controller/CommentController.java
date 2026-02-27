package org.shiningyang.mirrorheart_v2_2.module.community.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.community.dto.CommentCreateDto;
import org.shiningyang.mirrorheart_v2_2.module.community.dto.CommentVo;
import org.shiningyang.mirrorheart_v2_2.module.community.entity.Comment;
import org.shiningyang.mirrorheart_v2_2.module.community.service.impl.CommentServiceImpl;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentServiceImpl commentService;

    // 发表评论
    @PostMapping("/publish")
    public Result<String> publish(@RequestBody @Valid CommentCreateDto dto) {
        Long userId = SecurityUtils.getUserId();
        commentService.publishComment(userId, dto);
        return Result.success("评论成功");
    }

    // 获取一级评论列表 (根评论)
    @GetMapping("/root")
    public Result<IPage<CommentVo>> getRootComments(
            @RequestParam Long postId,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Comment> page = new Page<>(pageNo, pageSize);
        Long userId = SecurityUtils.getUserId();
        IPage<CommentVo> resultPage = commentService.getRootComments(page, postId, userId);
        
        Result<IPage<CommentVo>> result = Result.success(resultPage);
        if (resultPage.getRecords().isEmpty()) {
            result.setMessage("暂无评论");
        }
        return result;
    }

    // 获取二级评论列表 (子评论)
    @GetMapping("/child")
    public Result<IPage<CommentVo>> getChildComments(
            @RequestParam Long rootId,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Comment> page = new Page<>(pageNo, pageSize);
        Long userId = SecurityUtils.getUserId();
        IPage<CommentVo> resultPage = commentService.getChildComments(page, rootId, userId);
        
        Result<IPage<CommentVo>> result = Result.success(resultPage);
        if (resultPage.getRecords().isEmpty()) {
            result.setMessage("暂无回复");
        }
        return result;
    }

    // 删除评论
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getUserId();
        commentService.deleteComment(userId, id);
        return Result.success("删除成功");
    }
}