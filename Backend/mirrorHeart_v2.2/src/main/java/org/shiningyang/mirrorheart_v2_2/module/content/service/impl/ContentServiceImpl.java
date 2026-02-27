package org.shiningyang.mirrorheart_v2_2.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.module.content.dto.*;
import org.shiningyang.mirrorheart_v2_2.module.content.entity.*;
import org.shiningyang.mirrorheart_v2_2.module.content.mapper.ContentMapper;
import org.shiningyang.mirrorheart_v2_2.module.content.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentServiceImpl extends ServiceImpl<ContentMapper, Content> implements IContentService {

    // 注入所有子表的 Service
    private final IContentQuoteService quoteService;
    private final IContentArticleService articleService;
    private final IContentBookService bookService;
    private final IContentPaintingService paintingService;
    private final IContentMusicService musicService;
    private final IContentMovieService movieService;
    
    private final IContentTagRelationService tagRelationService;

    // --- 辅助方法：初始化 Content 主表对象并保存 ---
    private Content initContent(String type, String title, String summary, String coverUrl, String source) {
        Content content = new Content();
        content.setType(type);
        content.setTitle(title);
        content.setSummary(summary == null ? "" : summary);
        content.setCoverUrl(coverUrl == null ? "" : coverUrl);
        content.setSource(source == null ? "" : source);
        content.setStatus((byte) 1);
        content.setLikeCount(0);
        this.save(content);
        return content;
    }

    // --- 辅助方法：保存标签关联 ---
    private void saveTags(Long contentId, List<Long> tagIds) {
        if (tagIds != null && !tagIds.isEmpty()) {
            List<ContentTagRelation> relations = tagIds.stream().map(tagId -> {
                ContentTagRelation r = new ContentTagRelation();
                r.setContentId(contentId);
                r.setTagId(tagId);
                return r;
            }).collect(Collectors.toList());
            tagRelationService.saveBatch(relations);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addQuote(QuoteCreateDto dto) {
        Content content = initContent("QUOTE", dto.getTitle(), dto.getSummary(), dto.getCoverUrl(), dto.getSource());
        
        ContentQuote quote = new ContentQuote();
        quote.setContentId(content.getId());
        quote.setText(dto.getText());
        quote.setAuthor(dto.getAuthor());
        quoteService.save(quote);

        saveTags(content.getId(), dto.getTagIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addArticle(ArticleCreateDto dto) {
        Content content = initContent("ARTICLE", dto.getTitle(), dto.getSummary(), dto.getCoverUrl(), dto.getSource());

        ContentArticle article = new ContentArticle();
        article.setContentId(content.getId());
        article.setBody(dto.getBody());
        article.setAuthor(dto.getAuthor());
        articleService.save(article);

        saveTags(content.getId(), dto.getTagIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addBook(BookCreateDto dto) {
        Content content = initContent("BOOK", dto.getTitle(), dto.getSummary(), dto.getCoverUrl(), null);

        ContentBook book = new ContentBook();
        book.setContentId(content.getId());
        book.setIsbn(dto.getIsbn() == null ? "" : dto.getIsbn());
        book.setAuthor(dto.getAuthor());
        book.setPublisher(dto.getPublisher() == null ? "" : dto.getPublisher());
        bookService.save(book);

        saveTags(content.getId(), dto.getTagIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPainting(PaintingCreateDto dto) {
        Content content = initContent("PAINTING", dto.getTitle(), dto.getSummary(), dto.getCoverUrl(), null);

        ContentPainting painting = new ContentPainting();
        painting.setContentId(content.getId());
        painting.setPainter(dto.getPainter());
        painting.setYear(dto.getYear() == null ? "" : dto.getYear());
        paintingService.save(painting);

        saveTags(content.getId(), dto.getTagIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMusic(MusicCreateDto dto) {
        Content content = initContent("MUSIC", dto.getTitle(), dto.getSummary(), dto.getCoverUrl(), null);

        ContentMusic music = new ContentMusic();
        music.setContentId(content.getId());
        music.setArtist(dto.getArtist());
        music.setAudioUrl(dto.getAudioUrl());
        music.setDurationMs(dto.getDurationMs() == null ? 0 : dto.getDurationMs());
        music.setLyric(dto.getLyric());
        musicService.save(music);

        saveTags(content.getId(), dto.getTagIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMovie(MovieCreateDto dto) {
        Content content = initContent("MOVIE", dto.getTitle(), dto.getSummary(), dto.getCoverUrl(), null);

        ContentMovie movie = new ContentMovie();
        movie.setContentId(content.getId());
        movie.setDirector(dto.getDirector() == null ? "" : dto.getDirector());
        movie.setYear(dto.getYear() == null ? "" : dto.getYear());
        movieService.save(movie);

        saveTags(content.getId(), dto.getTagIds());
    }

    @Override
    public IPage<ContentListVo> getContentList(Page<Content> page, String type) {
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(type != null && !type.isEmpty(), Content::getType, type);
        wrapper.eq(Content::getStatus, 1);
        wrapper.orderByDesc(Content::getCreatedAt);

        Page<Content> contentPage = this.page(page, wrapper);

        return contentPage.convert(content -> {
            ContentListVo vo = new ContentListVo();
            BeanUtils.copyProperties(content, vo);
            return vo;
        });
    }

    @Override
    @Cacheable(value = "content:detail", key = "#id")
    public ContentDetailVo getContentDetail(Long id) {
        Content content = this.getById(id);
        if (content == null) {
            throw new CustomException("内容不存在");
        }

        ContentDetailVo vo = new ContentDetailVo();
        BeanUtils.copyProperties(content, vo);

        // 根据类型查子表 (多态处理)
        switch (content.getType()) {
            case "QUOTE":
                vo.setSpecificData(quoteService.getById(id));
                break;
            case "ARTICLE":
                vo.setSpecificData(articleService.getById(id));
                break;
            case "BOOK":
                vo.setSpecificData(bookService.getById(id));
                break;
            case "PAINTING":
                vo.setSpecificData(paintingService.getById(id));
                break;
            case "MUSIC":
                vo.setSpecificData(musicService.getById(id));
                break;
            case "MOVIE":
                vo.setSpecificData(movieService.getById(id));
                break;
            default:
                break;
        }

        return vo;
    }
}