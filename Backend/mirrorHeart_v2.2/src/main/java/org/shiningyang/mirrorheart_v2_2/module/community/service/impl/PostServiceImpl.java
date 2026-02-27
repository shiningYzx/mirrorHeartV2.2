package org.shiningyang.mirrorheart_v2_2.module.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.common.utils.SecurityUtils;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserInfoVo;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.shiningyang.mirrorheart_v2_2.module.auth.service.IUserService;
import org.shiningyang.mirrorheart_v2_2.module.community.dto.PostCreateDto;
import org.shiningyang.mirrorheart_v2_2.module.community.dto.PostVo;
import org.shiningyang.mirrorheart_v2_2.module.community.entity.*;
import org.shiningyang.mirrorheart_v2_2.module.community.mapper.PostMapper;
import org.shiningyang.mirrorheart_v2_2.module.community.mapper.PostViewHistoryMapper;
import org.shiningyang.mirrorheart_v2_2.module.community.service.*;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.FavoriteAction;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.LikeAction;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.UserRelation;
import org.shiningyang.mirrorheart_v2_2.module.interaction.mapper.FavoriteActionMapper;
import org.shiningyang.mirrorheart_v2_2.module.interaction.mapper.LikeActionMapper;
import org.shiningyang.mirrorheart_v2_2.module.interaction.mapper.UserRelationMapper;
import org.shiningyang.mirrorheart_v2_2.module.system.service.INotificationService;
import org.shiningyang.mirrorheart_v2_2.module.system.service.SensitiveWordService;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements IPostService {

    private final IPostImageService imageService;
    private final IPostAudioService audioService;
    private final IPostTagRelationService tagRelationService;
    private final IUserService userService;
    private final SensitiveWordService sensitiveWordService;
    private final INotificationService notificationService;
    private final UserRelationMapper userRelationMapper;
    private final LikeActionMapper likeActionMapper;
    private final FavoriteActionMapper favoriteActionMapper;
    // 🌟 新增：注入浏览历史 Mapper
    private final PostViewHistoryMapper postViewHistoryMapper;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPost(Long userId, PostCreateDto dto) {
        log.info("帖子上传记录{}", dto.toString());
        String titleMatch = sensitiveWordService.getFirstMatchWord(dto.getTitle());
        if (titleMatch != null) {
            throw new CustomException("标题包含违规词汇：" + titleMatch);
        }
        String textMatch = sensitiveWordService.getFirstMatchWord(dto.getText());
        if (textMatch != null) {
            throw new CustomException("正文包含违规词汇：" + textMatch);
        }

        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(dto.getTitle() == null ? "" : dto.getTitle());
        post.setText(dto.getText());
        post.setVisibility(dto.getVisibility() == null ? 0 : dto.getVisibility().byteValue());
        post.setStatus((byte) 1);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setFavoriteCount(0);
        post.setViewCount(0);
        post.setHeatScore(0L);
        this.save(post);

        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            List<PostImage> images = new ArrayList<>();
            for (int i = 0; i < dto.getImageUrls().size(); i++) {
                PostImage img = new PostImage();
                img.setPostId(post.getId());
                img.setUrl(dto.getImageUrls().get(i));
                img.setSort(i);
                images.add(img);
            }
            imageService.saveBatch(images);
        }

        if (dto.getAudioUrl() != null && !dto.getAudioUrl().isEmpty()) {
            PostAudio audio = new PostAudio();
            audio.setPostId(post.getId());
            audio.setUrl(dto.getAudioUrl());
            audio.setDurationMs(dto.getAudioDurationMs() == null ? 0 : dto.getAudioDurationMs());
            audioService.save(audio);
        }

        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            List<PostTagRelation> relations = dto.getTagIds().stream().map(tagId -> {
                PostTagRelation r = new PostTagRelation();
                r.setPostId(post.getId());
                r.setTagId(tagId);
                return r;
            }).collect(Collectors.toList());
            tagRelationService.saveBatch(relations);
        }

        if (post.getVisibility() <= 1) {
            String contentSnippet = (post.getTitle() != null && !post.getTitle().isEmpty())
                    ? post.getTitle() : post.getText();
            notificationService.notifyFollowersOnNewPost(userId, post.getId(), contentSnippet);
        }
    }

    // 🌟 1. 广场列表
    @Override
    public IPage<PostVo> getPostList(Page<Post> page, Long currentUserId, String sortType) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();

        if (currentUserId != null) {
            Set<Long> invisibleIds = getInvisibleUserIds(currentUserId);
            if (!invisibleIds.isEmpty()) {
                wrapper.notIn(Post::getUserId, invisibleIds);
            }
        }

        wrapper.eq(Post::getStatus, 1);
        wrapper.eq(Post::getVisibility, 0);
        wrapper.inSql(Post::getUserId, "SELECT id FROM user WHERE show_post = 1 AND status = 1");

        if ("hot".equals(sortType)) {
            wrapper.orderByDesc(Post::getHeatScore);
        } else {
            wrapper.orderByDesc(Post::getCreatedAt);
        }

        IPage<PostVo> voPage = this.page(page, wrapper).convert(this::assemblePostVo);
        // 🌟 优化：复用统一的批量装配方法
        batchAssembleInteractionStates(voPage.getRecords(), currentUserId);
        return voPage;
    }

    // 🌟 2. 搜索帖子
    @Override
    public IPage<PostVo> searchPosts(Page<Post> pageParam, String keyword, Long currentUserId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();

        if (currentUserId != null) {
            Set<Long> invisibleIds = getInvisibleUserIds(currentUserId);
            if (!invisibleIds.isEmpty()) {
                wrapper.notIn(Post::getUserId, invisibleIds);
            }
        }

        wrapper.eq(Post::getStatus, 1);
        wrapper.eq(Post::getVisibility, 0);
        wrapper.inSql(Post::getUserId, "SELECT id FROM user WHERE show_post = 1 AND status = 1");
        wrapper.and(w -> w.like(Post::getTitle, keyword).or().like(Post::getText, keyword));
        wrapper.orderByDesc(Post::getCreatedAt);

        IPage<PostVo> voPage = this.page(pageParam, wrapper).convert(this::assemblePostVo);
        // 🌟 优化：复用统一的批量装配方法
        batchAssembleInteractionStates(voPage.getRecords(), currentUserId);
        return voPage;
    }

    // 🌟 3. 获取特定用户主页的帖子 (修复了丢图、丢点赞状态的问题)
    @Override
    public IPage<PostVo> getUserPostList(Page<Post> page, Long targetUserId, Long currentUserId) {
        User targetUser = userService.getById(targetUserId);
        if (targetUser == null) throw new CustomException("目标用户不存在");

        boolean isSelf = currentUserId != null && currentUserId.equals(targetUserId);

        if (!isSelf && targetUser.getShowPost() != null && targetUser.getShowPost() == 0) {
            throw new CustomException("该用户已隐藏个人动态");
        }

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getUserId, targetUserId);

        if (!isSelf) {
            if (isFollower(currentUserId, targetUserId)) {
                wrapper.in(Post::getVisibility, 0, 1);
            } else {
                wrapper.eq(Post::getVisibility, 0);
            }
        }

        wrapper.orderByDesc(Post::getCreatedAt);

        // 🌟 修复：必须使用 assemblePostVo，否则就没有图片和音频
        IPage<PostVo> voPage = this.page(page, wrapper).convert(this::assemblePostVo);
        // 🌟 修复：补全获取点赞/收藏状态
        batchAssembleInteractionStates(voPage.getRecords(), currentUserId);

        return voPage;
    }

    // 🌟 4. 获取特定用户的收藏列表 (修复了丢点赞状态的问题)
    @Override
    public IPage<PostVo> getUserFavoritePostList(Page<Post> page, Long targetUserId, Long currentUserId) {
        User targetUser = userService.getById(targetUserId);
        if (targetUser == null) throw new CustomException("目标用户不存在");

        boolean isSelf = currentUserId != null && currentUserId.equals(targetUserId);

        if (!isSelf && targetUser.getShowFavorite() != null && targetUser.getShowFavorite() == 0) {
            throw new CustomException("该用户已隐藏收藏列表");
        }

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.inSql(Post::getId, "SELECT post_id FROM favorite_action WHERE user_id = " + targetUserId);

        if (!isSelf) {
            wrapper.eq(Post::getVisibility, 0);
            wrapper.inSql(Post::getUserId, "SELECT id FROM user WHERE show_post = 1");
        }

        wrapper.orderByDesc(Post::getCreatedAt);

        IPage<PostVo> voPage = this.page(page, wrapper).convert(this::assemblePostVo);
        // 🌟 修复：补全获取点赞/收藏状态
        batchAssembleInteractionStates(voPage.getRecords(), currentUserId);

        return voPage;
    }

    // 🌟 5. 单条详情
    @Override
    // ⚠️ 致命漏洞修复：去除了 @Cacheable，防止带私人状态的详情被全局缓存！
    public PostVo getPostDetail(Long postId, Long currentUserId) {
        Post post = this.getById(postId);
        if (post == null || post.getStatus() != 1) {
            throw new CustomException("帖子不存在或已被删除");
        }

        boolean isSelf = currentUserId != null && currentUserId.equals(post.getUserId());

        if (!isSelf) {
            User author = userService.getById(post.getUserId());
            if (author != null && author.getShowPost() != null && author.getShowPost() == 0) {
                throw new CustomException("作者已隐藏动态");
            }

            if (post.getVisibility() == 2) {
                throw new CustomException("抱歉，该帖子为作者私密，您无权查看");
            } else if (post.getVisibility() == 1) {
                if (!isFollower(currentUserId, post.getUserId())) {
                    throw new CustomException("该帖子仅粉丝可见，请先关注作者");
                }
            }
        }

        post.setViewCount(post.getViewCount() + 1);
        this.updateById(post);

        // ==========================================
        // 🌟 新增：利用子线程异步记录或更新浏览历史
        // ==========================================
        if (currentUserId != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    PostViewHistory history = postViewHistoryMapper.selectOne(new LambdaQueryWrapper<PostViewHistory>()
                            .eq(PostViewHistory::getUserId, currentUserId)
                            .eq(PostViewHistory::getPostId, postId));

                    if (history != null) {
                        // 如果之前看过，更新浏览时间将其顶到前面
                        history.setViewedAt(LocalDateTime.now());
                        postViewHistoryMapper.updateById(history);
                    } else {
                        // 首次浏览，插入新记录
                        history = new PostViewHistory();
                        history.setUserId(currentUserId);
                        history.setPostId(postId);
                        history.setViewedAt(LocalDateTime.now());
                        postViewHistoryMapper.insert(history);

                        // 【清理防膨胀】：保证单个用户最多只有 100 条浏览记录
                        Long count = postViewHistoryMapper.selectCount(new LambdaQueryWrapper<PostViewHistory>()
                                .eq(PostViewHistory::getUserId, currentUserId));
                        if (count > 100) {
                            List<PostViewHistory> oldRecords = postViewHistoryMapper.selectList(new LambdaQueryWrapper<PostViewHistory>()
                                    .eq(PostViewHistory::getUserId, currentUserId)
                                    .orderByDesc(PostViewHistory::getViewedAt)
                                    .last("LIMIT 100, 50")); // 取出 100 名开外的历史数据
                            if (!oldRecords.isEmpty()) {
                                postViewHistoryMapper.deleteBatchIds(oldRecords.stream().map(PostViewHistory::getId).collect(Collectors.toList()));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("异步记录浏览足迹失败", e);
                }
            });
        }

        PostVo vo = assemblePostVo(post);

        if (currentUserId != null) {
            boolean isLiked = likeActionMapper.exists(new LambdaQueryWrapper<LikeAction>()
                    .eq(LikeAction::getUserId, currentUserId)
                    .eq(LikeAction::getTargetType, "POST")
                    .eq(LikeAction::getTargetId, postId));
            vo.setIsLiked(isLiked);

            boolean isFavorited = favoriteActionMapper.exists(new LambdaQueryWrapper<FavoriteAction>()
                    .eq(FavoriteAction::getUserId, currentUserId)
                    .eq(FavoriteAction::getPostId, postId));
            vo.setIsFavorited(isFavorited);

            if (vo.getAuthor() != null) {
                if (vo.getAuthor().getId().equals(currentUserId)) {
                    vo.getAuthor().setIsFollowed(false);
                } else {
                    vo.getAuthor().setIsFollowed(isFollower(currentUserId, vo.getAuthor().getId()));
                }
            }
        }

        return vo;
    }

    // 6.查询用户的浏览足迹
    @Override
    public IPage<PostVo> getPostViewHistoryList(Page<Post> page, Long targetUserId) {
        Long currentUserId = SecurityUtils.getSafeUserId();
        if (currentUserId == null) {
            throw new CustomException("请先登录");
        }
        // 【安全拦截】：如果查询的不是自己的足迹，则必须验证是否拥有管理员权限
        if (!targetUserId.equals(currentUserId)) {
            User currentUser = userService.getById(currentUserId);
            if (currentUser == null || currentUser.getRole() != 1) {
                throw new CustomException("越权操作：您只能查看自己的浏览记录");
            }
        }

        // 1. 先查出浏览历史分页 (按浏览时间倒序)
        Page<PostViewHistory> historyPage = new Page<>(page.getCurrent(), page.getSize());
        postViewHistoryMapper.selectPage(historyPage, new LambdaQueryWrapper<PostViewHistory>()
                .eq(PostViewHistory::getUserId, currentUserId)
                .orderByDesc(PostViewHistory::getViewedAt));

        if (historyPage.getRecords().isEmpty()) {
            return new Page<>(page.getCurrent(), page.getSize(), 0);
        }

        // 2. 提取刚才查出的记录里的 Post IDs
        List<Long> postIds = historyPage.getRecords().stream()
                .map(PostViewHistory::getPostId)
                .collect(Collectors.toList());

        // 3. 构建帖子查询，利用 FIND_IN_SET / FIELD 保持之前的浏览时间倒序
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Post::getId, postIds);
        wrapper.eq(Post::getStatus, 1);

        // 过滤掉被拉黑的人发的帖子
        Set<Long> invisibleIds = getInvisibleUserIds(currentUserId);
        if (!invisibleIds.isEmpty()) {
            wrapper.notIn(Post::getUserId, invisibleIds);
        }

        // 足迹里的私密过滤：不能看别人后来改成“仅自己可见”的帖子
        wrapper.and(w -> w.eq(Post::getUserId, currentUserId).or().ne(Post::getVisibility, 2));

        // 保持 MySQL 原顺序输出
        String idStr = postIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        wrapper.last("ORDER BY FIELD(id, " + idStr + ")");

        List<Post> posts = this.list(wrapper);

        // 4. 内存中过滤粉丝可见权限，然后组装 VO
        List<PostVo> voList = posts.stream().filter(p -> {
            if (p.getVisibility() == 1 && !p.getUserId().equals(currentUserId)) {
                return isFollower(currentUserId, p.getUserId());
            }
            return true;
        }).map(this::assemblePostVo).collect(Collectors.toList());

        // 批量回填点赞/收藏状态
        batchAssembleInteractionStates(voList, currentUserId);

        Page<PostVo> voPage = new Page<>(page.getCurrent(), page.getSize(), historyPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }


    // 新增：修改帖子可见度逻辑
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePostVisibility(Long userId, Long postId, Integer visibility) {
        // 校验参数: 0=公开, 1=仅粉丝可见, 2=仅自己可见
        if (visibility == null || visibility < 0 || visibility > 2) {
            throw new CustomException("非法的可见度参数");
        }

        Post post = this.getById(postId);
        if (post == null) {
            throw new CustomException("帖子不存在或已被删除");
        }

        // 核心防御：只有作者本人才能修改可见度
        if (!post.getUserId().equals(userId)) {
            throw new CustomException("无权修改此帖子的可见度");
        }

        post.setVisibility(visibility.byteValue());
        this.updateById(post);
    }

    @Override
    @Transactional
    @CacheEvict(value = "post:detail", key = "#postId")
    public void deletePost(Long userId, Long postId) {
        Post post = this.getById(postId);
        if (post == null) {
            throw new CustomException("已删除，请勿重复");
        }
        if (!post.getUserId().equals(userId)) {
            throw new CustomException("无权删除此帖子");
        }
        this.removeById(postId);
    }

    // ==========================================
    // ⬇️ 以下为内部提取的通用方法，彻底消除冗余 ⬇️
    // ==========================================

    /**
     * 【核心提取】批量装配列表中的点赞、收藏、关注状态
     */
    private void batchAssembleInteractionStates(List<PostVo> records, Long currentUserId) {
        if (currentUserId == null || records.isEmpty()) return;

        List<Long> postIds = records.stream().map(PostVo::getId).collect(Collectors.toList());

        Set<Long> likedPostIds = likeActionMapper.selectList(new LambdaQueryWrapper<LikeAction>()
                .eq(LikeAction::getUserId, currentUserId).eq(LikeAction::getTargetType, "POST").in(LikeAction::getTargetId, postIds)
        ).stream().map(LikeAction::getTargetId).collect(Collectors.toSet());

        Set<Long> favoritedPostIds = favoriteActionMapper.selectList(new LambdaQueryWrapper<FavoriteAction>()
                .eq(FavoriteAction::getUserId, currentUserId).in(FavoriteAction::getPostId, postIds)
        ).stream().map(FavoriteAction::getPostId).collect(Collectors.toSet());

        Set<Long> authorIds = records.stream().filter(vo -> vo.getAuthor() != null)
                .map(vo -> vo.getAuthor().getId()).collect(Collectors.toSet());

        Set<Long> followedAuthorIds = authorIds.isEmpty() ? new HashSet<>() :
                userRelationMapper.selectList(new LambdaQueryWrapper<UserRelation>()
                        .eq(UserRelation::getFromUserId, currentUserId).eq(UserRelation::getType, 1).in(UserRelation::getToUserId, authorIds)
                ).stream().map(UserRelation::getToUserId).collect(Collectors.toSet());

        records.forEach(vo -> {
            vo.setIsLiked(likedPostIds.contains(vo.getId()));
            vo.setIsFavorited(favoritedPostIds.contains(vo.getId()));
            if (vo.getAuthor() != null) {
                vo.getAuthor().setIsFollowed(!vo.getAuthor().getId().equals(currentUserId) && followedAuthorIds.contains(vo.getAuthor().getId()));
            }
        });
    }

    /**
     * 获取不可见用户(互相拉黑)
     */
    private Set<Long> getInvisibleUserIds(Long currentUserId) {
        Set<Long> invisibleIds = new HashSet<>();
        if (currentUserId == null) return invisibleIds;

        userRelationMapper.selectList(new LambdaQueryWrapper<UserRelation>()
                        .eq(UserRelation::getFromUserId, currentUserId).eq(UserRelation::getType, 2))
                .forEach(r -> invisibleIds.add(r.getToUserId()));

        userRelationMapper.selectList(new LambdaQueryWrapper<UserRelation>()
                        .eq(UserRelation::getToUserId, currentUserId).eq(UserRelation::getType, 2))
                .forEach(r -> invisibleIds.add(r.getFromUserId()));

        return invisibleIds;
    }

    /**
     * 将 Post 组装为基础的 PostVo（包含多媒体和作者基础信息）
     */
    private PostVo assemblePostVo(Post post) {
        PostVo vo = new PostVo();
        BeanUtils.copyProperties(post, vo);
        vo.setVisibility(post.getVisibility().intValue());

        User author = userService.getById(post.getUserId());
        if (author != null) {
            vo.setAuthor(UserInfoVo.fromUser(author));
        }

        List<PostImage> images = imageService.list(new LambdaQueryWrapper<PostImage>()
                .eq(PostImage::getPostId, post.getId())
                .orderByAsc(PostImage::getSort));
        vo.setImageUrls(images.stream().map(PostImage::getUrl).collect(Collectors.toList()));

        PostAudio audio = audioService.getOne(new LambdaQueryWrapper<PostAudio>()
                .eq(PostAudio::getPostId, post.getId()));
        if (audio != null) {
            vo.setAudioUrl(audio.getUrl());
            vo.setAudioDurationMs(audio.getDurationMs());
        }
        return vo;
    }

    /**
     * 判断当前用户是否关注了目标用户
     */
    private boolean isFollower(Long currentUserId, Long targetUserId) {
        if (currentUserId == null) return false;
        return userRelationMapper.exists(new LambdaQueryWrapper<UserRelation>()
                .eq(UserRelation::getFromUserId, currentUserId).eq(UserRelation::getToUserId, targetUserId).eq(UserRelation::getType, 1));
    }
}