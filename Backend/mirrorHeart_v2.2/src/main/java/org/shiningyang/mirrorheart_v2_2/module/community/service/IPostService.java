package org.shiningyang.mirrorheart_v2_2.module.community.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.shiningyang.mirrorheart_v2_2.module.community.dto.PostCreateDto;
import org.shiningyang.mirrorheart_v2_2.module.community.dto.PostVo;
import org.shiningyang.mirrorheart_v2_2.module.community.entity.Post;

public interface IPostService extends IService<Post> {
    
    void createPost(Long userId, PostCreateDto dto);

    // 获取广场帖子列表 (传入当前用户ID鉴权，排序方式)
    IPage<PostVo> getPostList(Page<Post> page, Long userId, String sortType);

    // 获取帖子详情
    PostVo getPostDetail(Long postId, Long currentUserId);

    // 删除帖子
    void deletePost(Long userId, Long postId);

    // 搜索帖子
    IPage<PostVo> searchPosts(Page<Post> page, String keyword, Long currentUserId);

    // 新增：获取特定用户的帖子列表
    IPage<PostVo> getUserPostList(Page<Post> page, Long targetUserId, Long currentUserId);

    // 获取特定用户的收藏帖子列表
    IPage<PostVo> getUserFavoritePostList(Page<Post> page, Long targetUserId, Long currentUserId);

    // 新增：获取当前用户的帖子浏览历史 (足迹)
    IPage<PostVo> getPostViewHistoryList(Page<Post> page, Long currentUserId);

    // 新增：修改帖子可见度
    void updatePostVisibility(Long userId, Long postId, Integer visibility);
}