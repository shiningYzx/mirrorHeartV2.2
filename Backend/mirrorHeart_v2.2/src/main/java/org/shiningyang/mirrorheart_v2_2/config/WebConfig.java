package org.shiningyang.mirrorheart_v2_2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${file.access-path}")
    private String accessPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /files/** 映射到本地磁盘目录
        // 注意：file: 协议前缀不能少，最后的分隔符也不能少
        String location = "file:" + uploadPath;
        
        // 这里的 accessPath 是 /files/，映射规则需要 /files/**
        String pattern = accessPath + "**";
        
        registry.addResourceHandler(pattern)
                .addResourceLocations(location);
    }
}