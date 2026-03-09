package com.thy.cloud.data.cache.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Distributed lock annotation — apply to any Spring-managed method to
 * acquire a Redis-backed distributed lock before execution.
 * <p>
 * Supports SpEL expressions in {@link #key()} for dynamic key generation.
 *
 * <pre>
 * &#064;DistributedLock(prefix = "order", key = "#orderId", leaseTime = 30)
 * public void processOrder(String orderId) { ... }
 * </pre>
 *
 * @author Engin Mahmut
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * Lock key prefix — combined with {@link #key()} to form the full Redis key.
     * Final key format: {@code lock:{prefix}:{evaluated-key}}
     */
    String prefix() default "";

    /**
     * Lock key — supports SpEL expressions (e.g. {@code #userId}, {@code #order.id}).
     * If empty, defaults to the method's fully-qualified name.
     */
    String key() default "";

    /**
     * Maximum time to wait for lock acquisition before giving up.
     * A value of 0 means fail immediately if the lock is not available.
     */
    long waitTime() default 3;

    /**
     * Maximum time the lock is held before automatic release (lease/TTL).
     * Prevents deadlocks if the holder crashes.
     */
    long leaseTime() default 30;

    /**
     * Time unit for {@link #waitTime()} and {@link #leaseTime()}.
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * Error message thrown when lock acquisition fails.
     */
    String failMessage() default "Failed to acquire distributed lock. Please try again.";
}
