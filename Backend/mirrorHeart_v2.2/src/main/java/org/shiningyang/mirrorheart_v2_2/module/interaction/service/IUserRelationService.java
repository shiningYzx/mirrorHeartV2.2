package org.shiningyang.mirrorheart_v2_2.module.interaction.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.shiningyang.mirrorheart_v2_2.module.auth.dto.UserInfoVo;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.shiningyang.mirrorheart_v2_2.module.interaction.dto.BlockToggleDto;
import org.shiningyang.mirrorheart_v2_2.module.interaction.dto.FollowToggleDto;
import org.shiningyang.mirrorheart_v2_2.module.interaction.entity.UserRelation;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户关系表 服务类
 * </p>
 *
 * @author ShiningYang
 * @since 2026-02-20
 */
public interface IUserRelationService extends IService<UserRelation> {
    /**
     * 关注 / 取消关注
     * @return true=已关注, false=取消了关注
     */
    boolean toggleFollow(Long currentUserId, FollowToggleDto dto);

    /**
     * 拉黑 / 取消拉黑
     * @return true=已拉黑, false=取消了拉黑
     */
    boolean toggleBlock(Long currentUserId, BlockToggleDto dto);

    /**
     * 获取我关注的人列表
     */
    IPage<User> getMyFollowedList(Long currentUserId, Page<UserRelation> pageParam);

    // 新增：获取特定用户的关注列表 (返回组装好的 UserInfoVo)
    IPage<UserInfoVo> getUserFollowedList(Long targetUserId, Long currentUserId, Page<UserRelation> pageParam);

    // 新增：获取特定用户的粉丝列表
    IPage<UserInfoVo> getUserFollowerList(Long targetUserId, Long currentUserId, Page<UserRelation> pageParam);

    // 新增：获取我的拉黑列表 (黑名单)
    IPage<UserInfoVo> getMyBlockedList(Long currentUserId, Page<UserRelation> pageParam);
}
