package org.shiningyang.mirrorheart_v2_2.common.security;

import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shiningyang.mirrorheart_v2_2.common.result.Result;
import org.shiningyang.mirrorheart_v2_2.common.utils.JwtUtils;
import org.shiningyang.mirrorheart_v2_2.module.auth.entity.AuthSession;
import org.shiningyang.mirrorheart_v2_2.module.auth.mapper.AuthSessionMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final AuthSessionMapper authSessionMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // 【核心修复点】：认证公开接口绝对白名单放行
        // 如果是访问认证模块（登录/注册/验证码/刷新Token/注销等），直接跳过 JWT 拦截器逻辑。
        // 防止前端带着残留的过期 Token 来请求登录时，被下方逻辑直接拦截并返回 401 导致死锁。
        if (requestURI.startsWith("/api/v1/auth/")
        ) {
            chain.doFilter(request, response);
            return;
        }


        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String authToken = authHeader.substring(7);

            // 1. 校验令牌类型必须是 ACCESS
            String tokenType = jwtUtils.getTokenTypeFromToken(authToken);
            if (!"ACCESS".equals(tokenType)) {
                log.warn("拦截非法请求: 试图使用非 Access Token 访问业务接口");

                // 【特例放行】如果是请求刷新接口，允许放行交给 Controller 处理，防止前端将 RefreshToken 误放 Header 导致死锁
                if (!request.getRequestURI().contains("/auth/refresh")) {
                    outputErrorResponse(response, request,  401, "令牌类型错误：请使用 Access Token 访问该接口");
                    return; // 短路，直接返回 JSON，不再向下传递
                }
            } else {
                // 是正常的 ACCESS Token，继续验证
                String username = jwtUtils.getUsernameFromToken(authToken);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 2. 验证 Token 签名和过期
                    if (jwtUtils.validateToken(authToken, username)) {

                        // 3. 查验 auth_session，确保该 Token 没有被撤销
                        String jti = jwtUtils.getJtiFromToken(authToken);
                        boolean isSessionValid = false;

                        if (jti != null) {
                            AuthSession session = authSessionMapper.selectOne(new LambdaQueryWrapper<AuthSession>()
                                    .eq(AuthSession::getJti, jti));
                            isSessionValid = (session != null && session.getRevokedAt() == null);
                        }

                        if (isSessionValid) {
                            // 会话正常，赋予 Spring Security 上下文权限
                            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        } else {
                            log.warn("拦截无效会话: Token已在数据库作废 - User: {}", username);
                            // 【同步优化】如果令牌在数据库被标记为撤销（如异地顶号、主动退出），直接返回明确提示
                            outputErrorResponse(response, request, 401, "当前会话已失效或在其他设备登出，请重新登录");
                            return; // 短路，直接返回
                        }
                    } else {
                        log.warn("拦截过期令牌: - User: {}", username);
                        outputErrorResponse(response, request, 401, "登录凭证已过期，请重新登录");
                        return; // 短路，直接返回
                    }
                }
            }
        }

        // 放行（可能没有 Token 去访问白名单接口，也可能已经被正确赋权）
        chain.doFilter(request, response);
    }

    /**
     * 辅助方法：直接向 HttpServletResponse 输出规范化的 JSON 错误结果
     */
    private void outputErrorResponse(HttpServletResponse response, HttpServletRequest request,int code, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // HTTP 状态码设为 401
        response.setContentType("application/json;charset=utf-8");

        // 🌟【核心修复】：手动加上跨域头，防止浏览器因为 CORS 拦截而隐藏 401 状态码
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        } else {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");

        // 使用 Hutool 的 JSONConfig 强制保留 null 值字段，使得返回体中包含 "data": null
        JSONConfig jsonConfig = JSONConfig.create().setIgnoreNullValue(false);
        String jsonStr = JSONUtil.toJsonStr(Result.error(code, msg), jsonConfig);

        // 利用 Hutool 的 JSONUtil 把 Result 对象转成字符串返回
        response.getWriter().write(jsonStr);
        response.getWriter().flush();
        response.getWriter().close();
    }
}