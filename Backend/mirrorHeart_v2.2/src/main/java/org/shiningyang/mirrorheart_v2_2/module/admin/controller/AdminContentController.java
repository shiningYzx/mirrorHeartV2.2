package org.shiningyang.mirrorheart_v2_2.module.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.annotation.AuditLog;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.module.content.dto.*;
import org.shiningyang.mirrorheart_v2_2.module.content.service.IContentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端：内容库管理 API
 */
@RestController
@RequestMapping("/api/v1/admin/content")
@RequiredArgsConstructor
public class AdminContentController {

    private final IContentService contentService;

    @PostMapping("/quote")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "内容管理", operation = "发布金句")
    public Result<String> addQuote(@RequestBody @Valid QuoteCreateDto dto) {
        contentService.addQuote(dto);
        return Result.success("金句发布成功");
    }

    @PostMapping("/article")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "内容管理", operation = "发布文章")
    public Result<String> addArticle(@RequestBody @Valid ArticleCreateDto dto) {
        contentService.addArticle(dto);
        return Result.success("文章发布成功");
    }

    @PostMapping("/book")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "内容管理", operation = "发布书籍")
    public Result<String> addBook(@RequestBody @Valid BookCreateDto dto) {
        contentService.addBook(dto);
        return Result.success("书籍发布成功");
    }

    @PostMapping("/painting")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "内容管理", operation = "发布画作")
    public Result<String> addPainting(@RequestBody @Valid PaintingCreateDto dto) {
        contentService.addPainting(dto);
        return Result.success("画作发布成功");
    }

    @PostMapping("/music")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "内容管理", operation = "发布音乐")
    public Result<String> addMusic(@RequestBody @Valid MusicCreateDto dto) {
        contentService.addMusic(dto);
        return Result.success("音乐发布成功");
    }

    @PostMapping("/movie")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = "内容管理", operation = "发布电影")
    public Result<String> addMovie(@RequestBody @Valid MovieCreateDto dto) {
        contentService.addMovie(dto);
        return Result.success("电影发布成功");
    }
}