package org.shiningyang.mirrorheart_v2_2.common.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.shiningyang.mirrorheart_v2_2.module.auth.mapper.UserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义用户加载逻辑
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. 查询用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 2. 查询权限 (暂时 Mock，后续从 admin_user_role 表加载)
        List<String> permissions = new ArrayList<>();
        if (user.getRole() == 1) {
            permissions.add("ROLE_ADMIN"); // 管理员赋予特殊角色
            // TODO: 阶段二后期完善：查询 admin_permission 表加载具体权限
        } else {
            permissions.add("ROLE_USER"); // 普通用户
        }

        return new LoginUser(user, permissions);
    }
}