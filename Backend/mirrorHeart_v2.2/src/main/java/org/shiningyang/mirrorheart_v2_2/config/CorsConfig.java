package org.shiningyang.mirrorheart_v2_2.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 允许所有接口
                .allowedOriginPatterns("*") // 允许所有前端地址跨域
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的请求方式
                .allowedHeaders("*") // 允许所有的请求头
                .allowCredentials(true) // 允许携带 Cookie/Token
                .maxAge(3600); // 预检请求的有效期
    }
}