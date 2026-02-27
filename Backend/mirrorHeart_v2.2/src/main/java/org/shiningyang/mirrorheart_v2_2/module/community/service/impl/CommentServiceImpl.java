package org.shiningyang.mirrorheart_v2_2.module.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserInfoVo;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.shiningyang.mirrorheart_v2_2.module.auth.service.IUserService;
import org.shiningyang.mirrorheart_v2_2.module.community.dto.CommentCreateDto;
import org.shiningyang.mirrorheart_v2_2.module.community.dto.CommentVo;
import org.shiningyang.mirrorheart_v2_2.module.community.entity.Comment;
import org.shiningyang.mirrorheart_v2_2.module.community.entity.Post;
import org.shiningyang.mirrorheart_v2_2.module.community.mapper.CommentMapper;
import org.shiningyang.mirrorheart_v2_2.module.community.service.ICommentService;
import org.shiningyang.mirrorheart_v2_2.module.community.service.IPostService;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.LikeAction;
import org.shiningyang.mirrorheart_v2_2.module.interaction.mapper.LikeActionMapper;
import org.shiningyang.mirrorheart_v2_2.module.system.service.INotificationService;
import org.shiningyang.mirrorheart_v2_2.module.system.service.SensitiveWordService;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {

    private final IPostService postService;
    private final IUserService userService;

    // [新增] 注入通知服务
    private final INotificationService notificationService;
    private final LikeActionMapper likeActionMapper; // 注入点赞 Mapper

    private final SensitiveWordService sensitiveWordService; // [新增] 注入敏感词服务
    // [新增] 注入 Spring CacheManager 用于编程式清除缓存
    private final CacheManager cacheManager;
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "post:detail", key = "#dto.postId") // [新增] 发表评论后，清除对应帖子的详情缓存 (更新评论数)
    public void publishComment(Long userId, CommentCreateDto dto) {
        // [新增] 敏感词校验
        String textMatch = sensitiveWordService.getFirstMatchWord(dto.getText());
        if (textMatch != null) {
            throw new CustomException("评论包含违规词汇：" + textMatch);
        }

        Post post = postService.getById(dto.getPostId());
        if (post == null) {
            throw new CustomException("帖子不存在");
        }

        Comment comment = new Comment();
        comment.setPostId(dto.getPostId());
        comment.setUserId(userId);
        comment.setText(dto.getText());
        comment.setParentId(dto.getParentId());
        comment.setRootId(dto.getRootId());
        comment.setLikeCount(0);
        comment.setIsDeleted((byte) 0);

        this.save(comment);

        // 发布评论：数量+1，热度+10
        postService.lambdaUpdate()
                .setSql("comment_count = comment_count + 1, heat_score = heat_score + 10")
                .eq(Post::getId, post.getId())
                .update();

        // [新增] 发送评论通知
        // 1. 给帖子作者发通知
        notificationService.createNotification(post.getUserId(), userId, "COMMENT", "POST", post.getId(), post.getTitle());

        // 2. 如果是回复评论 (二级评论)，还要给原评论作者发通知
        if (dto.getParentId() != null) {
            Comment parentComment = this.getById(dto.getParentId());
            // 确保父评论存在，且接收者不是帖子作者(避免重复发两条)，也不是评论者自己
            if (parentComment != null
                    && !parentComment.getUserId().equals(post.getUserId())
                    && !parentComment.getUserId().equals(userId)) {
                notificationService.createNotification(parentComment.getUserId(), userId, "REPLY", "COMMENT", parentComment.getId(), parentComment.getText());
            }
        }
    }

    // ... (后续方法保持不变: getRootComments, getChildComments, deleteComment, assembleCommentVo) ...
    @Override
    public IPage<CommentVo> getRootComments(Page<Comment> page, Long postId, Long currentUserId) {
        // 1. 查询该帖子下的一级评论 (假设 rootId 为空代表一级评论)
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, postId)
                .isNull(Comment::getRootId)
                .orderByDesc(Comment::getLikeCount) // 按点赞数优先，然后按时间
                .orderByDesc(Comment::getCreatedAt);

        Page<Comment> commentPage = this.page(page, wrapper);
        IPage<CommentVo> voPage = commentPage.convert(this::assembleCommentVo);

        List<CommentVo> records = voPage.getRecords();
        if (records.isEmpty()) {
            return voPage;
        }

        // 提取当前页所有一级评论的 ID
        List<Long> rootIds = records.stream().map(CommentVo::getId).collect(Collectors.toList());

        // 🌟 2. 【核心性能优化】批量统计子评论数 (childCount)
        QueryWrapper<Comment> countWrapper = new QueryWrapper<>();
        countWrapper.select("root_id", "count(id) as child_count")
                .in("root_id", rootIds)
                .groupBy("root_id");

        // 得到类似：[{root_id: 1, child_count: 5}, {root_id: 2, child_count: 2}] 的结果
        List<Map<String, Object>> countMaps = this.baseMapper.selectMaps(countWrapper);
        Map<Long, Long> childCountMap = new HashMap<>();
        for (Map<String, Object> map : countMaps) {
            Long rootId = ((Number) map.getOrDefault("root_id", map.get("ROOT_ID"))).longValue();
            Long count = ((Number) map.getOrDefault("child_count", map.get("CHILD_COUNT"))).longValue();
            childCountMap.put(rootId, count);
        }

        // 3. 批量查询当前登录用户的点赞状态
        Set<Long> likedIds = new HashSet<>();
        if (currentUserId != null) {
            likedIds = likeActionMapper.selectList(new LambdaQueryWrapper<LikeAction>()
                    .eq(LikeAction::getUserId, currentUserId)
                    .eq(LikeAction::getTargetType, "COMMENT")
                    .in(LikeAction::getTargetId, rootIds)
            ).stream().map(LikeAction::getTargetId).collect(Collectors.toSet());
        }

        // 4. 内存回填 childCount 和 isLiked
        final Set<Long> finalLikedIds = likedIds;
        records.forEach(vo -> {
            vo.setChildCount(childCountMap.getOrDefault(vo.getId(), 0L));
            vo.setIsLiked(finalLikedIds.contains(vo.getId()));
        });

        return voPage;
    }

    @Override
    public IPage<CommentVo> getChildComments(Page<Comment> page, Long rootId, Long currentUserId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getRootId, rootId)
                .orderByAsc(Comment::getCreatedAt); // 子评论一般按时间正序排列

        Page<Comment> commentPage = this.page(page, wrapper);
        IPage<CommentVo> voPage = commentPage.convert(this::assembleCommentVo);

        List<CommentVo> records = voPage.getRecords();
        if (!records.isEmpty()) {

            // 1. 批量查询当前登录用户的点赞状态
            if (currentUserId != null) {
                List<Long> commentIds = records.stream().map(CommentVo::getId).collect(Collectors.toList());
                Set<Long> likedIds = likeActionMapper.selectList(new LambdaQueryWrapper<LikeAction>()
                        .eq(LikeAction::getUserId, currentUserId)
                        .eq(LikeAction::getTargetType, "COMMENT")
                        .in(LikeAction::getTargetId, commentIds)
                ).stream().map(LikeAction::getTargetId).collect(Collectors.toSet());

                records.forEach(vo -> vo.setIsLiked(likedIds.contains(vo.getId())));
            }

            // ==========================================
            // 🌟 2. 【核心新增】：批量装配“回复给谁”的信息
            // ==========================================

            // 提取所有子评论的 parentId
            // (过滤掉 parentId == rootId 的，因为直接回复楼主不需要显示"回复@xxx")
            Set<Long> parentIds = records.stream()
                    .map(CommentVo::getParentId)
                    .filter(pid -> pid != null && !pid.equals(rootId))
                    .collect(Collectors.toSet());

            if (!parentIds.isEmpty()) {
                // A. 查询这些被回复的评论记录，找出它们的作者 ID
                List<Comment> parentComments = this.listByIds(parentIds);
                Map<Long, Long> parentToUserIdMap = parentComments.stream()
                        .collect(Collectors.toMap(Comment::getId, Comment::getUserId));

                // B. 提取所有的被回复作者 ID
                Set<Long> replyUserIds = new HashSet<>(parentToUserIdMap.values());

                if (!replyUserIds.isEmpty()) {
                    // C. 批量查询被回复作者的用户信息
                    List<User> replyUsers = userService.listByIds(replyUserIds);
                    Map<Long, User> userMap = replyUsers.stream()
                            .collect(Collectors.toMap(User::getId, u -> u));

                    // D. 内存回填给 VO
                    records.forEach(vo -> {
                        Long pid = vo.getParentId();
                        if (pid != null && parentToUserIdMap.containsKey(pid)) {
                            Long uid = parentToUserIdMap.get(pid);
                            User u = userMap.get(uid);
                            if (u != null) {
                                // 同样使用 fromUser 保护隐私信息
                                vo.setReplyToUser(UserInfoVo.fromUser(u));
                            }
                        }
                    });
                }
            }
        }

        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new CustomException("已删除，请勿重复");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new CustomException("无权删除");
        }
        this.removeById(commentId);
        postService.lambdaUpdate()
                .setSql("comment_count = comment_count - 1, heat_score = heat_score - 10")
                .eq(Post::getId, comment.getPostId())
                .update();

        // [新增] 编程式清除该帖子的详情缓存
        Cache cache = cacheManager.getCache("post:detail");
        if (cache != null) {
            cache.evict(comment.getPostId());
        }
    }

    private CommentVo assembleCommentVo(Comment comment) {
        CommentVo vo = new CommentVo();
        BeanUtils.copyProperties(comment, vo);
        User author = userService.getById(comment.getUserId());
        if (author != null) {
            vo.setAuthor(UserInfoVo.fromUser(author));
        }
        if (comment.getRootId() == null) {
            Long childCount = this.count(new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getRootId, comment.getId()));
            vo.setChildCount(childCount);
        }
        return vo;
    }

    /**
     * 🌟【新增辅助方法】通用：为评论列表批量装配点赞状态
     */
    private void assembleLikeStatusForComments(List<CommentVo> commentVos, Long currentUserId) {
        if (currentUserId == null || commentVos == null || commentVos.isEmpty()) {
            return;
        }

        List<Long> commentIds = commentVos.stream()
                .map(CommentVo::getId)
                .collect(Collectors.toList());

        Set<Long> likedCommentIds = likeActionMapper.selectList(new LambdaQueryWrapper<LikeAction>()
                .eq(LikeAction::getUserId, currentUserId)
                .eq(LikeAction::getTargetType, "COMMENT") // targetType 对应评论
                .in(LikeAction::getTargetId, commentIds)
        ).stream().map(LikeAction::getTargetId).collect(Collectors.toSet());

        commentVos.forEach(vo -> {
            // 确保 CommentVo 中已经有 isLiked 字段
            vo.setIsLiked(likedCommentIds.contains(vo.getId()));
        });
    }
}