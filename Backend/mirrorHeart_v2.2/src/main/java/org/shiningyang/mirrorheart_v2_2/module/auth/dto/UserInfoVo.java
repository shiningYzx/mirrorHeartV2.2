package org.shiningyang.mirrorheart_v2_2.module.auth.dto;

import lombok.Data;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserInfoVo {
    private Long id;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private Integer role;

    private Boolean isFollowed = false;
    private Integer postCount;      // 帖子数量
    private Integer followingCount; // 关注数
    private Integer followerCount;  // 粉丝数

    // 🌟 新增：用户在设置页修改隐私选项时传入
    private Byte showFollowing;
    private Byte showFavorite;
    private Byte showPost;

    // 新增：向前端透传生日信息
    private LocalDate birthday;
    private LocalDateTime birthdayUpdateTime;
    /**
     * 🌟 策略 1：供外部调用的【公开视角】构造器
     * (用于：帖子作者、评论者、他人主页、粉丝列表等)
     * 作用：强制抹除核心隐私数据，防止抓包泄露
     */
    public static UserInfoVo fromUser(User user) {
        UserInfoVo vo = new UserInfoVo();
        BeanUtils.copyProperties(user, vo);
        vo.setRole(user.getRole() != null ? user.getRole().intValue() : 0);

        // 🛡️ 强制脱敏
        vo.setEmail(null);
        vo.setBirthday(null);
        vo.setBirthdayUpdateTime(null);

        return vo;
    }

    /**
     * 🌟 策略 2：供查询【自己主页】时使用的【私人视角】构造器
     * (用于：个人中心)
     * 作用：保留完整的真实数据，供前端回显和编辑
     */
    public static UserInfoVo fromSelf(User user) {
        UserInfoVo vo = new UserInfoVo();
        BeanUtils.copyProperties(user, vo);
        vo.setRole(user.getRole() != null ? user.getRole().intValue() : 0);
        return vo;
    }
}