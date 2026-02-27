package org.shiningyang.mirrorheart_v2_2.module.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserUpdateDto {

    @Size(min = 1, max = 64, message = "昵称长度限制1-64字符")
    private String nickname;

    private String avatarUrl;

    @Size(max = 255, message = "简介不能超过255字")
    private String bio;

    // 🌟 新增：用户在设置页修改隐私选项时传入
    private Byte showFollowing;
    private Byte showFavorite;
    private Byte showPost;

    // 🌟 新增：生日相关字段
    private LocalDate birthday;
    private LocalDateTime birthdayUpdateTime;
}