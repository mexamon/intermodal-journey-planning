package com.thy.cloud.data.cache.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

import java.time.Duration;

/**
 * Cache key package
 *
 * @author Engin Mahmut
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CacheKey {
    /**
     * redis key
     */
    @NonNull
    private String key;
    /**
     *Timeout seconds
     */
    private Duration expire;

    public CacheKey(final @NonNull String key) {
        this.key = key;
    }

}
