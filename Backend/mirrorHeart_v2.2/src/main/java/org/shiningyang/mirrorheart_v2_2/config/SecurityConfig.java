package org.shiningyang.mirrorheart_v2_2.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import org.shiningyang.mirrorheart_v2_2.common.result.ErrorCode;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.shiningyang.mirrorheart_v2_2.common.security.JwtAuthenticationTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 开启 @PreAuthorize 注解
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationTokenFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // 禁用 CSRF (JWT 不需要)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 禁用 Session
            .authorizeHttpRequests(auth -> auth
                // 放行白名单
                .requestMatchers("/api/v1/auth/**").permitAll() // 登录注册接口
                .requestMatchers("/files/**").permitAll() // 获取文件
                .requestMatchers("/api/v1/system/version/**").permitAll() // 版本接口

                // 集中网关路由鉴权：所有 /api/v1/admin/** 路径必须拥有 ADMIN 角色
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // 🌟 【修复403报错核心】：同时放行 ERROR 和 ASYNC 类型的内部转发！
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()

                // 放行游客可以访问的业务 GET 接口
                .requestMatchers(HttpMethod.GET, "/api/v1/post/list/**").permitAll() // 帖子列表
                .requestMatchers(HttpMethod.GET, "/api/v1/post/{id}").permitAll()    // 帖子详情
                .requestMatchers(HttpMethod.GET, "/api/v1/comment/**").permitAll()   // 评论列表
                .requestMatchers(HttpMethod.GET, "/api/v1/question/**").permitAll()  // 每日一问及回答
                .requestMatchers(HttpMethod.GET, "/api/v1/search/**").permitAll()    // 搜索接口
                .requestMatchers(HttpMethod.GET, "/api/v1/user/profile/**").permitAll() // 查看他人主页
                .requestMatchers(HttpMethod.GET, "/api/v1/recommend/today/**").permitAll() // 游客每日推荐


                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // 跨域预检
                .anyRequest().authenticated() // 其他所有请求都需要认证
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class) // 添加 JWT 过滤器
            
            // 配置异常处理 (返回 JSON 而不是 HTML)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(unauthorizedEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // 处理 401 未认证
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new ObjectMapper().writeValueAsString(Result.error(ErrorCode.UNAUTHORIZED)));
        };
    }

    // 处理 403 无权限
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(new ObjectMapper().writeValueAsString(Result.error(ErrorCode.FORBIDDEN)));
        };
    }
}