package org.shiningyang.mirrorheart_v2_2.common.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security 身份主体
 */
@Data
public class LoginUser implements UserDetails {

    private User user;
    
    // 权限列表
    private List<String> permissions;

    // 缓存 Spring Security 需要的权限对象
    @JsonIgnore
    private List<SimpleGrantedAuthority> authorities;

    public LoginUser(User user, List<String> permissions) {
        this.user = user;
        this.permissions = permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (authorities != null) {
            return authorities;
        }
        // 把字符串权限转为 GrantedAuthority
        authorities = permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != 0; // 0=禁用
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == 1; // 1=正常
    }
}