package org.shiningyang.mirrorheart_v2_2.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class JwtUtils {

    // 密钥，生产环境应该配置在 application.yml 中
    @Value("${jwt.secret:MirrorHeartSecretKeyDoNotLeakMirrorHeartSecretKey}")
    private String secret;

    // 过期时间，默认 7 天
    @Value("${jwt.expiration:604800}")
    private Long expiration;

    /**
     * 生成自定义过期时间、JTI 和类型的 Token (双Token机制核心)
     * @param tokenType "ACCESS" 或 "REFRESH"
     */
    public String generateToken(Long userId, String username, Long customExpirationSeconds, String jti, String tokenType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("sub", username);
        claims.put("created", new Date());
        claims.put("type", tokenType); // 注入 Token 的类型

        return Jwts.builder()
                .setClaims(claims)
                .setId(jti) // 植入 JWT 唯一身份标识 JTI
                .setExpiration(generateExpirationDate(customExpirationSeconds))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 从 Token 中获取用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Long userId;
        try {
            final Claims claims = getClaimsFromToken(token);
            userId = Long.valueOf(Objects.requireNonNull(claims).get("userId").toString());
        } catch (Exception e) {
            userId = null;
        }
        return userId;
    }

    /**
     * 从 Token 中获取 JWT ID (JTI)
     */
    public String getJtiFromToken(String token) {
        try {
            return Objects.requireNonNull(getClaimsFromToken(token)).getId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 Token 中获取 Token 类型 (ACCESS / REFRESH)
     */
    public String getTokenTypeFromToken(String token) {
        try {
            return getClaimsFromToken(token).get("type", String.class);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * 从 Token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        String username;
        try {
            final Claims claims = getClaimsFromToken(token);
            username = Objects.requireNonNull(claims).getSubject();
        } catch (Exception e) {
            username = null;
        }
        return username;
    }

    private Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.warn("JWT格式验证失败: {}", token);
            return null;
        }
    }

    private Date generateExpirationDate(Long customExpirationSeconds) {
        return new Date(System.currentTimeMillis() + customExpirationSeconds * 1000);
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean validateToken(String token, String username) {
        final String usernameInToken = getUsernameFromToken(token);
        return (usernameInToken.equals(username) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    private Date getExpirationDateFromToken(String token) {
        return Objects.requireNonNull(getClaimsFromToken(token)).getExpiration();
    }
}