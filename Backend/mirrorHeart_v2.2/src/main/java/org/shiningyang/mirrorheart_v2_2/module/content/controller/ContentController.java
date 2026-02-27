package org.shiningyang.mirrorheart_v2_2.module.content.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.module.content.dto.*;
import org.shiningyang.mirrorheart_v2_2.module.content.service.IContentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {

    private final IContentService contentService;
    // --- 用户端/公共接口 ---

    @GetMapping("/list")
    public Result<IPage<ContentListVo>> getContentList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String type) {

        Page<org.shiningyang.mirrorheart_v2_2.module.content.entity.Content> page = new Page<>(pageNo, pageSize);
        return Result.success(contentService.getContentList(page, type));
    }

    @GetMapping("/{id}")
    public Result<ContentDetailVo> getContentDetail(@PathVariable Long id) {
        return Result.success(contentService.getContentDetail(id));
    }
}