package com.thy.cloud.data.cache.util;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.lang.NonNull;

import java.time.Duration;

import static com.thy.cloud.base.util.str.StrPool.COLON;

/**
 * Hash cache key package
 *
 * @author Engin Mahmut
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CacheHashKey extends CacheKey {

    /**
     * redis hash field
     */
    @NonNull
    private Object field;

    public CacheHashKey(@NonNull String key, final @NonNull Object field) {
        super(key);
        this.field = field;
    }

    public CacheHashKey(@NonNull String key, final @NonNull Object field, Duration expire) {
        super(key, expire);
        this.field = field;
    }

    public CacheKey tran() {
        return new CacheKey(String.join(COLON, getKey(), getField().toString()), getExpire());
    }
}
