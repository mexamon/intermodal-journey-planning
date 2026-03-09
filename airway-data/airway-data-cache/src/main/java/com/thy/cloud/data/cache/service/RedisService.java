package com.thy.cloud.data.cache.service;

import com.thy.cloud.data.cache.util.CacheKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Generic Cache Repository
 * @see RedisTemplate
 *
 * @author Engin Mahmut
 */
@SuppressWarnings(value = { "unchecked"})
@Component
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * java.time.Duration compatibility
     *
     * @param duration duration parameter
     * @return has millis
     */
    private static boolean hasMillis(Duration duration) {
        return duration.toMillis() % 1000L != 0L;
    }

    /**
     * Cache basic objects, Integer, String, entity classes, etc.
     *
     * @param key cache key value
     * @param value cached value
     */
    public <T> void setCacheObject(final String key, final T value)
    {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * Cache basic objects, Integer, String, entity classes, etc.
     *
     * @param key cache key value
     * @param value cached value
     * @param timeout time
     * @param timeUnit time granularity
     */
    public <T> void setCacheObject(final String key, final T value, final Long timeout, final TimeUnit timeUnit)
    {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * Set valid time
     *
     * @param key Redis key
     * @param timeout timeout time
     * @return true=setting succeeded; false=setting failed
     */
    public boolean expire(final String key, final long timeout)
    {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * Set valid time
     *
     * @param key Redis key
     * @param timeout timeout time
     * @param unit time unit
     * @return true=setting succeeded; false=setting failed
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit)
    {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * CacheKey duration to timeUnit compatibility
     * @param key
     * @return
     */
    private void expire(CacheKey key) {
            Assert.notNull(key.getExpire(), "Timeout must not be null");
            if (hasMillis(key.getExpire())) {
                redisTemplate.expire(key.getKey(), key.getExpire().toMillis(), TimeUnit.MILLISECONDS);
            } else {
                redisTemplate.expire(key.getKey(), key.getExpire().getSeconds(), TimeUnit.SECONDS);
            }
       }

    /**
     *
     * To be used for generic repository
     *
     * @param key CacheKey compatibility
     */
    private void setExpire(CacheKey key) {
        if (key != null && key.getExpire() != null) {
            expire(key);
        }
    }

    /**
     * Get valid time
     *
     * @param key Redis key
     * @return valid time
     */
    public long getExpire(final String key)
    {
        return redisTemplate.getExpire(key);
    }

    /**
     * Determine if the key exists
     *
     * @param key key
     * @return true exists false does not exist
     */
    public Boolean hasKey(String key)
    {
        return redisTemplate.hasKey(key);
    }

    /**
     * Get the cached base object.
     *
     * @param key cache key value
     * @return cache the data corresponding to the key value
     */
    public <T> T getCacheObject(final String key)
    {
        ValueOperations<String, T> operation = (ValueOperations<String, T>) redisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * delete a single object
     *
     * @param key
     */
    public void deleteObject(final String key)
    {
        redisTemplate.delete(key);
    }

    /**
     * delete the collection
     * @param collection multiple objects
     * @return
     */
    public <T> void deleteObject(final Collection<T> collection)
    {
        redisTemplate.delete((Collection<String>) collection);
    }

    /**
     * Cache List data
     *
     * @param key cache key value
     * @param dataList List data to be cached
     * @return the cached object
     */
    public <T> long setCacheList(final String key, final List<T> dataList)
    {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    /**
     * Get the cached list object
     *
     * @param key cache key value
     * @return cache the data corresponding to the key value
     */
    public <T> List<T> getCacheList(final String key)
    {
        return (List<T>) redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * Cache Set
     *
     * @param key cache key value
     * @param dataSet cached data
     * @return the object of the cached data
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet)
    {
        BoundSetOperations<String, T> setOperation = (BoundSetOperations<String, T>) redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext())
        {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    /**
     * Get the cached set
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheSet(final String key)
    {
        return (Set<T>) redisTemplate.opsForSet().members(key);
    }

    /**
     * Cache Map
     *
     * @param key
     * @param dataMap
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap)
    {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * Get the cached Map
     *
     * @param key
     * @return
     */
    public Map<Object, Object> getCacheMap(final String key)
    {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * Store data in Hash
     *
     * @param key Redis key
     * @param hKey Hash key
     * @param value value
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value)
    {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * Get the data in Hash
     *
     * @param key Redis key
     * @param hKey Hash key
     * @return object in Hash
     */
    public <T> T getCacheMapValue(final String key, final String hKey)
    {
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    /**
     * Get data from multiple hashes
     *
     * @param key Redis key
     * @param hKeys Hash key collection
     * @return Hash object collection
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys)
    {
        return (List<T>) redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    /**
     * Get a list of cached base objects
     *
     * @param pattern string prefix
     * @return list of objects
     */
    public Collection<String> keys(final String pattern)
    {
        return redisTemplate.keys(pattern);
    }

}
