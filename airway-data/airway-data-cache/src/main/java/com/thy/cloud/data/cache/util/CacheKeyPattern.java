package com.thy.cloud.data.cache.util;

import cn.hutool.core.convert.Convert;
import com.thy.cloud.base.util.str.StrPool;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;

import static com.thy.cloud.base.util.str.StrPool.COLON;

/**
 * Cache Key Builder
 *
 * @author Engin Mahmut
 */
@FunctionalInterface
public interface CacheKeyPattern {

    /**
     * Get Cache Key Prefix
     *
     * @return key prefix
     */
    @NonNull
    String getPrefix();

    /**
     * Cache Duration
     *
     * @return timeout
     */
    @Nullable
    default Duration getExpire() {
        return null;
    }

    /**
     * Build a cache key for general KV mode
     *
     * @param suffix parameter
     * @return cache key
     */
    default CacheKey key(Object... suffix) {
        String field = suffix.length > 0 ? Convert.toStr(suffix[0], StrPool.EMPTY) : StrPool.EMPTY;
        return hashFieldKey(field, suffix);
    }

    /**
     * Build a hash cache key of redis type
     *
     * @param field field
     * @param suffix dynamic parameter
     * @return cache key
     */
    default CacheHashKey hashFieldKey(@NonNull Object field, Object... suffix) {
        String key = getKey(suffix);
        Assert.notNull(key, "key cannot be empty");
        Assert.notNull(field, "field cannot be empty");
        return new CacheHashKey(key, field, getExpire());
    }

    /**
     * According to dynamic parameters splicing parameters
     *
     * @param suffix dynamic parameter
     * @return cache key
     */
    default String getKey(Object... suffix) {
        ArrayList<String> keyFieldList = new ArrayList<>();
        String prefix = this.getPrefix();
        Assert.notNull(prefix, "The cache prefix cannot be empty");
        keyFieldList.add(prefix);
        Arrays.stream(suffix).filter(ObjectUtils::isNotEmpty).map(String::valueOf).forEach(keyFieldList::add);
        return String.join(COLON, keyFieldList);
    }

}
