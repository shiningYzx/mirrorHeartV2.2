package org.shiningyang.mirrorheart_v2_2.module.recommend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.module.content.dto.ContentDetailVo;
import org.shiningyang.mirrorheart_v2_2.module.content.dto.ContentListVo;
import org.shiningyang.mirrorheart_v2_2.module.content.entity.Content;
import org.shiningyang.mirrorheart_v2_2.module.content.service.IContentService;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.LikeAction;
import org.shiningyang.mirrorheart_v2_2.module.interaction.mapper.LikeActionMapper;
import org.shiningyang.mirrorheart_v2_2.module.recommend.dto.DailyRecommendationVo;
import org.shiningyang.mirrorheart_v2_2.module.recommend.entity.DailyRecommendation;
import org.shiningyang.mirrorheart_v2_2.module.recommend.entity.DailyRecommendationItem;
import org.shiningyang.mirrorheart_v2_2.module.recommend.mapper.DailyRecommendationMapper;
import org.shiningyang.mirrorheart_v2_2.module.recommend.service.IDailyRecommendationItemService;
import org.shiningyang.mirrorheart_v2_2.module.recommend.service.IDailyRecommendationService;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRecommendationServiceImpl extends ServiceImpl<DailyRecommendationMapper, DailyRecommendation> implements IDailyRecommendationService {

    private final IDailyRecommendationItemService itemService;
    private final IContentService contentService;
    private final LikeActionMapper likeActionMapper; //  新增：注入点赞 Mapper

    // 定义每日推荐包含的内容类型
    private static final List<String> RECOMMEND_TYPES = Arrays.asList(
            "QUOTE", "ARTICLE", "BOOK", "PAINTING", "MUSIC", "MOVIE"
    );

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyRecommendationVo getOrGenerateTodayRecommendation(Long userId) {
        LocalDate today = LocalDate.now();

        // 【核心】：如果 userId 为 null，我们将查询目标指向虚拟游客 ID: 0L
        Long queryUserId = (userId == null) ? 0L : userId;

        // 1. 查询是否已存在今日推荐
        DailyRecommendation recommendation = this.getOne(new LambdaQueryWrapper<DailyRecommendation>()
                .eq(DailyRecommendation::getUserId, queryUserId)
                .eq(DailyRecommendation::getDay, today));

        if (recommendation == null) {
            // 2. 如果不存在，生成新的推荐
            recommendation = generateRecommendation(queryUserId, today);
        } else {
            // 记录查看时间(如果之前未读)
            if (recommendation.getViewedAt() == null) {
                recommendation.setViewedAt(LocalDateTime.now());
                this.updateById(recommendation);
            }
        }

        // 这里传入的是原始的 userId (如果是游客就是 null)
        // 这样 buildVo 里面就不会去查点赞状态，isLiked 默认全是 false
        return buildVo(recommendation, userId);
    }

    /**
     * [新增] 按日期查询推荐
     */
    @Override
    // 修改缓存的 key，当 userId 为 null 时，存为 'guest'，防止缓存 key 报错
    @Cacheable(value = "recommendation:date:v2", key = "#userId + ':' + #date.toString()", unless = "#result == null")
    public DailyRecommendationVo getRecommendationByDate(Long userId, LocalDate date) {
        if (date.isEqual(LocalDate.now())) {
            return getOrGenerateTodayRecommendation(userId);
        }
        if (date.isAfter(LocalDate.now())) {
            throw new CustomException("无法查看未来的推荐");
        }

        // 查询指定日期的记录
        DailyRecommendation recommendation = this.getOne(new LambdaQueryWrapper<DailyRecommendation>()
                .eq(DailyRecommendation::getUserId, userId)
                .eq(DailyRecommendation::getDay, date));

        if (recommendation == null) {
            return null; // 当日无数据
        }
        return buildVo(recommendation, userId);
    }

    /**
     * [新增] 分页查询历史推荐列表
     */
    @Override
    public IPage<DailyRecommendationVo> getHistoryList(Page<DailyRecommendation> page, Long userId) {
        // 查主表，按日期倒序
        Page<DailyRecommendation> recPage = this.page(page, new LambdaQueryWrapper<DailyRecommendation>()
                .eq(DailyRecommendation::getUserId, userId)
                .le(DailyRecommendation::getDay, LocalDate.now()) // 不含未来
                .orderByDesc(DailyRecommendation::getDay));

        // 转换每一个记录为详细的 VO
        return recPage.convert(rec -> buildVo(rec, userId));
    }

    /**
     * 生成推荐核心逻辑
     */
    private DailyRecommendation generateRecommendation(Long userId, LocalDate today) {
        // 1. 创建主表记录
        DailyRecommendation rec = new DailyRecommendation();
        rec.setUserId(userId);
        rec.setDay(today);
        rec.setStrategy("random_v1"); // 目前策略：全随机
        rec.setViewedAt(LocalDateTime.now());
        this.save(rec);

        List<DailyRecommendationItem> items = new ArrayList<>();

        // 2. 为每种类型随机选一个内容
        for (int i = 0; i < RECOMMEND_TYPES.size(); i++) {
            String type = RECOMMEND_TYPES.get(i);
            
            // 简单随机算法：利用 SQL 的 lastSql("ORDER BY RAND() LIMIT 1")
            Content content = contentService.getOne(new LambdaQueryWrapper<Content>()
                    .eq(Content::getType, type)
                    .eq(Content::getStatus, 1) // 必须是已发布的
                    .last("ORDER BY RAND() LIMIT 1")); // 注意：数据量极大时性能有隐患，初期可用

            if (content != null) {
                DailyRecommendationItem item = new DailyRecommendationItem();
                item.setDailyId(rec.getId());
                item.setContentId(content.getId());
                item.setRank(i + 1); // 排序 1-6
                items.add(item);
            }
        }

        // 3. 批量插入子表
        if (!items.isEmpty()) {
            itemService.saveBatch(items);
        } else {
            // 如果库里完全没内容，可能会生成空推荐，这里可以抛异常或者容忍
            log.warn("今日推荐生成为空，可能是内容库无数据。UserId: {}", userId);
        }

        return rec;
    }

    /**
     * 组装返回数据 (VO)
     */
    private DailyRecommendationVo buildVo(DailyRecommendation rec, Long userId) {
        DailyRecommendationVo vo = new DailyRecommendationVo();
        BeanUtils.copyProperties(rec, vo);

        // 1. 查出关联的 Items
        List<DailyRecommendationItem> items = itemService.list(new LambdaQueryWrapper<DailyRecommendationItem>()
                .eq(DailyRecommendationItem::getDailyId, rec.getId())
                .orderByAsc(DailyRecommendationItem::getRank));

        // 🌟 修改：容器变为 ContentDetailVo
        List<ContentDetailVo> contentVos = new ArrayList<>();

        for (DailyRecommendationItem item : items) {
            try {
                // 🌟 核心：直接调用内容服务，获取带有 specificData (分表数据) 的详情对象
                ContentDetailVo contentVo = contentService.getContentDetail(item.getContentId());

                // 动态装配点赞状态
                if (userId != null) {
                    boolean isLiked = likeActionMapper.exists(new LambdaQueryWrapper<LikeAction>()
                            .eq(LikeAction::getUserId, userId)
                            .eq(LikeAction::getTargetType, "CONTENT")
                            .eq(LikeAction::getTargetId, contentVo.getId()));
                    contentVo.setIsLiked(isLiked);
                } else {
                    contentVo.setIsLiked(false);
                }

                contentVos.add(contentVo);
            } catch (CustomException e) {
                // 容错处理：如果推荐池里的某条内容被管理员下架或删除了，跳过该条展示
                log.warn("推荐项加载失败，可能是内容已被下架: ContentID={}", item.getContentId());
            }
        }
        vo.setItems(contentVos);
        return vo;
    }
}