package org.shiningyang.mirrorheart_v2_2.module.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.shiningyang.mirrorheart_v2_2.module.community.dto.CommentCreateDto;
import org.shiningyang.mirrorheart_v2_2.module.community.dto.CommentVo;
import org.shiningyang.mirrorheart_v2_2.module.community.entity.Comment;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 评论表 服务类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-18
 */
public interface ICommentService extends IService<Comment> {
    void publishComment(Long userId, CommentCreateDto dto);
    IPage<CommentVo> getRootComments(Page<Comment> page, Long postId, Long currentUserId);
    IPage<CommentVo> getChildComments(Page<Comment> page, Long rootId, Long currentUserId);
    void deleteComment(Long userId, Long commentId);
}
