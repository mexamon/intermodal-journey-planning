package com.thy.cloud.data.cache.config;

import com.thy.cloud.base.util.str.StrPool;
import com.thy.cloud.data.cache.properties.CacheProperties;
import com.thy.cloud.data.cache.properties.SerializerType;
import com.thy.cloud.data.cache.serializer.RedisObjectSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;

/**
 * Custom Redis Auto Configuration — Spring Boot 4 / Jackson 3 / Spring Data Redis 4
 *
 * @author Engin Mahmut
 */
@EnableCaching
@Configuration
@ConditionalOnProperty(name = CacheProperties.PREFIX + ".enabled", matchIfMissing = true)
@EnableConfigurationProperties({DataRedisProperties.class, CacheProperties.class})
@RequiredArgsConstructor
public class CacheAutoConfiguration {

    private final CacheProperties cacheProperties;

    private final DataRedisProperties redisProperties;

    /**
     * Lettuce Connection Factory
     */
    @Bean
    protected LettuceConnectionFactory redisConnectionFactory() {

        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
                .master(cacheProperties.getSentinelMaster());
        cacheProperties.getSentinelNodes().forEach(s -> sentinelConfig.sentinel(s, redisProperties.getPort()));
        sentinelConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));

        return new LettuceConnectionFactory(sentinelConfig, LettuceClientConfiguration.defaultConfiguration());
    }

    /**
     *  Custom Redis Serializer Bean from Configuration
     * @return RedisSerializer
     */
    @Bean
    @ConditionalOnMissingBean(RedisSerializer.class)
    public RedisSerializer<Object> redisSerializer(ObjectMapper objectMapper) {
        SerializerType serializerType = cacheProperties.getSerializerType();
        if (SerializerType.JDK == serializerType) {
            ClassLoader classLoader = this.getClass().getClassLoader();
            return new JdkSerializationRedisSerializer(classLoader);
        }
        return new RedisObjectSerializer(objectMapper);
    }

    /**
     * RedisTemplate configuration
     *
     * @param factory redis lettuce connection factory
     */
    @Bean("redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory factory, RedisSerializer<Object> redisSerializer, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        setRedisTemplateConfiguration(factory, template, redisSerializer, objectMapper);
        return template;
    }

    /**
     * Spring @Cacheable annotation configuration
     *
     * @param lettuceConnectionFactory connection factory
     * @return cache manager
     */
    @Bean(name = "cacheManager")
    @Primary
    public CacheManager cacheManager(LettuceConnectionFactory lettuceConnectionFactory) {
        RedisCacheConfiguration cacheConfiguration = cacheConfiguration();
        cacheConfiguration.entryTtl(cacheProperties.getGlobalConfig().getTimeToLive());

        Map<String, CacheProperties.Cache> configs = cacheProperties.getConfigs();
        Map<String, RedisCacheConfiguration> map = new HashMap<>();
        //Custom cache expiration time configuration
        Optional.ofNullable(configs).ifPresent(config ->
                config.forEach((key, cache) -> {
                    RedisCacheConfiguration cfg = setRedisCacheConfiguration(cache, cacheConfiguration);
                    map.put(key, cfg);
                })
        );
        return RedisCacheManager.builder(lettuceConnectionFactory)
                .cacheDefaults(cacheConfiguration)
                .withInitialCacheConfigurations(map)
                .build();
    }

    /**
     * Serializer Configuration
     */
    private void setRedisTemplateConfiguration(LettuceConnectionFactory factory, RedisTemplate<String, Object> template, RedisSerializer<Object> redisSerializer, ObjectMapper objectMapper) {
        RedisSerializer<?> stringSerializer = new StringRedisSerializer();
        template.setDefaultSerializer(new RedisObjectSerializer(objectMapper));
        template.setConnectionFactory(factory);
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(redisSerializer);
        template.setValueSerializer(redisSerializer);
        template.afterPropertiesSet();
    }

    /**
     * Handle Redis Configuration
     * @param redisProperties custom configuration properties
     * @param config spring data redis configuration
     * @return RedisCacheConfiguration for Spring Cache Manager
     */
    private RedisCacheConfiguration setRedisCacheConfiguration(CacheProperties.Cache redisProperties, RedisCacheConfiguration config) {
        if (Objects.isNull(redisProperties)) {
            return config;
        }
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (redisProperties.getKeyPrefix() != null) {
            config = config.computePrefixWith((String cacheName) -> redisProperties.getKeyPrefix().concat(StrPool.COLON).concat(cacheName).concat(StrPool.COLON));
        } else {
            config = config.computePrefixWith(cacheName -> cacheName.concat(StrPool.COLON));
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }

        return config;
    }

    /**
     * Cache Configuration
     */
    private RedisCacheConfiguration cacheConfiguration() {
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new RedisObjectSerializer(JsonMapper.builder().build())));
        return setRedisCacheConfiguration(cacheProperties.getGlobalConfig(), cacheConfiguration);
    }

    @Bean
    public KeyGenerator keyGenerator() {
        return (target, method, objects) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getName());
            sb.append(StrPool.COLON).append(method.getName()).append(StrPool.COLON);
            Arrays.stream(objects).map(Object::toString).forEach(sb::append);
            return sb.toString();
        };
    }

}
