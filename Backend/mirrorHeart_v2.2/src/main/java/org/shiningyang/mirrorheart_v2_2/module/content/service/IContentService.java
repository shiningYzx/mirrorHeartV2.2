package org.shiningyang.mirrorheart_v2_2.module.content.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.shiningyang.mirrorheart_v2_2.module.content.dto.*;
import org.shiningyang.mirrorheart_v2_2.module.content.entity.Content;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 内容主表 服务类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
public interface IContentService extends IService<Content> {
    void addQuote(QuoteCreateDto dto);
    void addArticle(ArticleCreateDto dto);
    void addBook(BookCreateDto dto);
    void addMusic(MusicCreateDto dto);
    void addPainting(PaintingCreateDto dto);
    void addMovie(MovieCreateDto dto);

    // [新增] 分页查询列表
    IPage<ContentListVo> getContentList(Page<Content> page, String type);

    // [新增] 获取详情
    ContentDetailVo getContentDetail(Long id);
}
