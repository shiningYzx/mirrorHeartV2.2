package org.shiningyang.mirrorheart_v2_2.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "tencent.cos")
public class CosConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(CosConfig.class);
    
    private String secretId;
    private String secretKey;
    private String region;
    private String bucketName;
    private String baseUrl;

    @Bean
    public COSClient cosClient() {
        logger.info("开始初始化腾讯云COS客户端");
        
        // 1 初始化用户身份信息（secretId, secretKey）
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        logger.debug("已初始化用户身份信息，secretId: {}", secretId != null ? secretId.substring(0, Math.min(4, secretId.length())) + "****" : "null");
        
        // 2 设置 bucket 的地域
        Region cosRegion = new Region(region);
        ClientConfig clientConfig = new ClientConfig(cosRegion);
        logger.debug("已设置bucket地域: {}", region);
        
        // 这里建议设置使用 https 协议
        clientConfig.setHttpProtocol(HttpProtocol.https);
        logger.debug("已设置HTTP协议为HTTPS");
        
        // 3 生成 cos 客户端
        COSClient cosClient = new COSClient(cred, clientConfig);
        logger.info("腾讯云COS客户端初始化成功");
        
        return cosClient;
    }
}