package org.shiningyang.mirrorheart_v2_2.module.content.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.module.content.dto.TagCreateDto;
import org.shiningyang.mirrorheart_v2_2.module.content.entity.Tag;
import org.shiningyang.mirrorheart_v2_2.module.content.service.ITagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tag")
@RequiredArgsConstructor
public class TagController {

    private final ITagService tagService;

    // --- 公开接口 ---

    @GetMapping("/list")
    public Result<List<Tag>> getTagList(@RequestParam(required = false) String group) {
        // 调用重构后的 Service 方法，享受缓存加速
        return Result.success(tagService.getTagsByGroup(group));
    }

    // --- 管理端接口 (实际应加 @PreAuthorize) ---

    @PostMapping("/add")
    public Result<String> addTag(@RequestBody TagCreateDto dto) {
        Tag tag = new Tag();
        tag.setName(dto.getName());
        tag.setGroupName(dto.getGroupName() == null ? "default" : dto.getGroupName());
        tag.setUseCount(0);

        // 调用带有 @CacheEvict 的方法
        tagService.addTag(tag);
        return Result.success("标签添加成功");
    }
}