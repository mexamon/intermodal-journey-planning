package com.thy.cloud.data.cache.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 *
 * Jet Cache Configuration Properties
 *
 * @author Engin Mahmut
 *
 */
@Data
@ConfigurationProperties(prefix = CacheProperties.PREFIX)
public class CacheProperties {

    public static final String PREFIX = "thy.cache.redis";

    /**
     * Whether to enable
     */
    private boolean enable = false;

    /**
     * Serializer Type
     */
    private SerializerType serializerType = SerializerType.JSON;

    /**
     * Sentinel Master Name
     */
    private String sentinelMaster;
    /**
     * Sentinel Password
     */
    private String sentinelPassword;

    /**
     * Sentinel Nodes
     */
    private List<String> sentinelNodes;

    /**
     * Redis Database Number
     */
    private Integer database = 0;

    /**
     * Global configuration
     */
    private Cache globalConfig = new Cache();

    /**
     * Special configuration for certain specific keys
     *
     * The key of configs needs to be configured as the value of the @Cacheable annotation
     */
    private Map<String, Cache> configs;

    @Data
    public static class Cache {

        /**
         * Key expiration time
         * Default expires in 1 hour
         */
        private Duration timeToLive = Duration.ofHours(1);

        /**
         * Allow caching of null values
         */
        private boolean cacheNullValues = true;

        /**
         * key prefix
         *
         * The final key format: keyPrefix + @ Cacheable.value + @ Cacheable.key
         *
         * Usage scenario: development / test environment or demonstration / production environment.
         * In order to save resources, they often share a redis, which can be distinguished according to the key prefix
         * (of course, it can also be achieved by switching the database) */
        private String keyPrefix;

        /**
         * Whether to use key prefix when writing redis
         */
        private boolean useKeyPrefix = true;

    }


}
