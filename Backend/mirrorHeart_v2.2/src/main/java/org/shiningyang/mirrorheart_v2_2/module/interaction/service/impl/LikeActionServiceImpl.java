package org.shiningyang.mirrorheart_v2_2.module.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shiningyang.mirrorheart_v2_2.common.exception.CustomException;
import org.shiningyang.mirrorheart_v2_2.module.community.entity.Comment;
import org.shiningyang.mirrorheart_v2_2.module.community.entity.Post;
import org.shiningyang.mirrorheart_v2_2.module.community.service.ICommentService;
import org.shiningyang.mirrorheart_v2_2.module.community.service.IPostService;
import org.shiningyang.mirrorheart_v2_2.module.content.entity.Content;
import org.shiningyang.mirrorheart_v2_2.module.content.service.IContentService;
import org.shiningyang.mirrorheart_v2_2.module.interaction.dto.LikeToggleDto;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.LikeAction;
import org.shiningyang.mirrorheart_v2_2.module.interaction.mapper.LikeActionMapper;
import org.shiningyang.mirrorheart_v2_2.module.interaction.service.ILikeActionService;
import org.shiningyang.mirrorheart_v2_2.module.question.entity.UserDailyRecord;
import org.shiningyang.mirrorheart_v2_2.module.question.mapper.UserDailyRecordMapper;
import org.shiningyang.mirrorheart_v2_2.module.system.service.INotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LikeActionServiceImpl extends ServiceImpl<LikeActionMapper, LikeAction> implements ILikeActionService {

    private final IPostService postService;
    private final ICommentService commentService;
    private final IContentService contentService;
    private final UserDailyRecordMapper userDailyRecordMapper;

    // [新增] 注入通知服务
    private final INotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long userId, LikeToggleDto dto) {
        String targetType = dto.getTargetType(); // "POST" 或 "COMMENT"
        Long targetId = dto.getTargetId();

        checkTargetExists(targetType, targetId);

        // 查询是否已点赞
        LikeAction likeAction = this.getOne(new LambdaQueryWrapper<LikeAction>()
                .eq(LikeAction::getUserId, userId)
                .eq(LikeAction::getTargetType, targetType)
                .eq(LikeAction::getTargetId, targetId));

        boolean isLike;
        if (likeAction != null) {
            this.removeById(likeAction.getId());
            updateLikeCount(targetType, targetId, -1);
            isLike = false;
        } else {
            likeAction = new LikeAction();
            likeAction.setUserId(userId);
            likeAction.setTargetType(targetType);
            likeAction.setTargetId(targetId);
            this.save(likeAction);
            updateLikeCount(targetType, targetId, 1);
            isLike = true;

            // [新增] 触发点赞通知
            sendLikeNotification(userId, dto);
        }
        return isLike;
    }

    private void sendLikeNotification(Long senderId, LikeToggleDto dto) {
        Long receiverId = null;
        String content = "";

        // 根据不同类型找到接收者(作者)
        switch (dto.getTargetType()) {
            case "POST":
                Post post = postService.getById(dto.getTargetId());
                if (post != null) {
                    receiverId = post.getUserId();
                    // content = post.getTitle().isEmpty() ? post.getText() : post.getTitle();
                    content = "赞了你的帖子";
                }
                break;
            case "COMMENT":
                Comment comment = commentService.getById(dto.getTargetId());
                if (comment != null) {
                    receiverId = comment.getUserId();
                    // content = comment.getText();
                    content = "赞了你的评论";
                }
                break;
            case "DAILY_ANSWER":
                UserDailyRecord answer = userDailyRecordMapper.selectById(dto.getTargetId());
                if (answer != null) {
                    receiverId = answer.getUserId();
                    content = "赞了你的每日一问回答";
                }
                break;
            default:
                // 内容库(Content)一般是官方发布的，暂不发通知
                break;
        }

        if (receiverId != null) {
            notificationService.createNotification(receiverId, senderId, "LIKE", dto.getTargetType(), dto.getTargetId(), content);
        }
    }

    private void checkTargetExists(String targetType, Long targetId) {
        boolean exists = false;
        switch (targetType) {
            case "POST": exists = postService.getById(targetId) != null; break;
            case "COMMENT": exists = commentService.getById(targetId) != null; break;
            case "CONTENT": exists = contentService.getById(targetId) != null; break;
            case "DAILY_ANSWER": exists = userDailyRecordMapper.selectById(targetId) != null; break;
            default: throw new CustomException("不支持的点赞类型: " + targetType);
        }
        if (!exists) throw new CustomException("点赞目标不存在或已被删除");
    }

    private void updateLikeCount(String targetType, Long targetId, int delta) {
        // 1. 🌟 给【帖子】专属的 SQL：点赞数同步增减，且热度 ±5
        String postSql = delta > 0
                ? "like_count = like_count + 1, heat_score = heat_score + 5"
                : "like_count = like_count - 1, heat_score = heat_score - 5";

        // 2. 🌟 给【其他内容】(如评论、每日推荐Content、回答等) 的 SQL：只更新点赞数
        String normalSql = delta > 0
                ? "like_count = like_count + 1"
                : "like_count = like_count - 1";

        // 3. 根据类型分配执行
        switch (targetType) {
            case "POST":
                // 只有帖子用 postSql
                postService.lambdaUpdate().setSql(postSql).eq(Post::getId, targetId).update();
                break;

            case "COMMENT":
                // 评论用 normalSql
                commentService.lambdaUpdate().setSql(normalSql).eq(Comment::getId, targetId).update();
                break;

            case "DAILY_ANSWER":
                // 每日一问回答用 normalSql
//                userDailyRecordService.lambdaUpdate().setSql(normalSql).eq(UserDailyRecord::getId, targetId).update();
                userDailyRecordMapper
                        .update(
                                null,
                                new LambdaUpdateWrapper<UserDailyRecord>()
                                        .setSql(normalSql).eq(UserDailyRecord::getId, targetId)
                        );
                break;

            // 注意：下面这里的 case 请根据你实际定义的 targetType 补充
            case "QUOTE":
            case "ARTICLE":
            case "BOOK":
            case "MOVIE":
            case "MUSIC":
            case "PAINTING":
            case "CONTENT": // 统称的 Content
                // 每日推荐的底层内容用 normalSql
                contentService.lambdaUpdate().setSql(normalSql).eq(Content::getId, targetId).update();
                break;

            default:
                log.warn("未知的点赞目标类型: {}", targetType);
                break;
        }
    }
}