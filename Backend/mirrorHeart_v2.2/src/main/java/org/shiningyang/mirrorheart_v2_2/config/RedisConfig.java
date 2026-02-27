package org.shiningyang.mirrorheart_v2_2.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching // 开启 Spring Cache 缓存注解支持
public class RedisConfig {

    /**
     * 配置 RedisTemplate，自定义序列化方式
     * 默认是 JDK 序列化（保存到 Redis 会乱码），这里改为 JSON 序列化
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 使用 String 序列化
        template.setKeySerializer(new StringRedisSerializer());
        // Value 使用 GenericJackson2JsonRedisSerializer 序列化为 JSON
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置 Spring Cache 的 CacheManager
     * 作用于 @Cacheable 等注解
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 1. 创建自定义的 ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        // 2. 指定可见性
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 3. 必须配置这个，否则反序列化时会将 JSON 转化为 Map，而不是具体的实体类
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);

        // 4. 🌟【核心修复】注册 Java 8 的时间模块，支持 LocalDateTime 的序列化与反序列化
        objectMapper.registerModule(new JavaTimeModule());

        // 5. 将 LocalDateTime 转化为格式化的字符串 (例如 "2023-10-01T12:00:00")，而不是时间戳数组
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 6. 使用配置好的 ObjectMapper 创建序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 7. 配置 RedisCacheConfiguration
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)) // 默认缓存时间设为 2 小时，可根据业务修改
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())) // Key 使用 String 序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer)) // Value 使用自定义的 JSON 序列化
                .disableCachingNullValues(); // 不缓存 null 值，防止缓存穿透

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(config)
                .build();
    }
}