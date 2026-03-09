package com.thy.cloud.data.cache.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Production-grade distributed lock implementation using Redis.
 * <p>
 * Uses Lua scripts for atomic lock acquisition and release, preventing
 * race conditions and ensuring only the lock owner can release it.
 * <p>
 * Features:
 * <ul>
 *     <li>Atomic lock/unlock via Lua scripts (no TOCTOU bugs)</li>
 *     <li>Owner identification via UUID token (prevents accidental release by other threads)</li>
 *     <li>Configurable TTL / lease time (prevents deadlocks on holder crash)</li>
 *     <li>Spin-wait with configurable timeout and backoff</li>
 *     <li>Thread-safe — each call generates a unique owner token</li>
 * </ul>
 *
 * @author Engin Mahmut
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDistributedLockService {

    private static final String LOCK_PREFIX = "lock:";

    /**
     * Lua script for atomic lock acquisition.
     * SET key value NX PX milliseconds — only sets if key doesn't exist.
     * Returns 1 if lock acquired, 0 otherwise.
     */
    private static final String LOCK_SCRIPT = """
            if redis.call('SET', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]) then
                return 1
            else
                return 0
            end
            """;

    /**
     * Lua script for atomic lock release.
     * Only deletes if the stored value matches the owner token —
     * prevents releasing a lock held by another process.
     * Returns 1 if released, 0 if lock not held or owned by someone else.
     */
    private static final String UNLOCK_SCRIPT = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            else
                return 0
            end
            """;

    /**
     * Lua script for lock renewal (extend TTL while still holding the lock).
     * Only extends if the stored value matches the owner token.
     * Returns 1 if renewed, 0 if lock not held.
     */
    private static final String RENEW_SCRIPT = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('PEXPIRE', KEYS[1], ARGV[2])
            else
                return 0
            end
            """;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Try to acquire a distributed lock with spin-wait.
     *
     * @param key       lock key (will be prefixed with "lock:")
     * @param waitTime  maximum time to wait for lock acquisition
     * @param leaseTime maximum time the lock is held (auto-release TTL)
     * @param timeUnit  time unit for waitTime and leaseTime
     * @return a {@link LockToken} if acquired, or {@code null} if timed out
     */
    public LockToken tryLock(String key, long waitTime, long leaseTime, TimeUnit timeUnit) {
        String lockKey = LOCK_PREFIX + key;
        String ownerToken = UUID.randomUUID().toString();
        long leaseMillis = timeUnit.toMillis(leaseTime);
        long waitMillis = timeUnit.toMillis(waitTime);
        long deadline = System.currentTimeMillis() + waitMillis;

        log.debug("Attempting lock: key={}, owner={}, leaseMs={}, waitMs={}",
                lockKey, ownerToken, leaseMillis, waitMillis);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(LOCK_SCRIPT, Long.class);

        // Spin-wait with exponential backoff
        long backoff = 50; // start at 50ms
        do {
            Long result = redisTemplate.execute(script,
                    Collections.singletonList(lockKey),
                    ownerToken, String.valueOf(leaseMillis));

            if (result != null && result == 1L) {
                log.debug("Lock acquired: key={}, owner={}", lockKey, ownerToken);
                return new LockToken(lockKey, ownerToken, leaseMillis);
            }

            // Wait before retrying
            try {
                Thread.sleep(Math.min(backoff, 500)); // cap at 500ms
                backoff = Math.min(backoff * 2, 500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Lock acquisition interrupted: key={}", lockKey);
                return null;
            }
        } while (System.currentTimeMillis() < deadline);

        log.warn("Lock acquisition timed out: key={}, waited={}ms", lockKey, waitMillis);
        return null;
    }

    /**
     * Try to acquire a lock immediately (no waiting).
     *
     * @param key       lock key
     * @param leaseTime maximum hold time
     * @param timeUnit  time unit
     * @return a {@link LockToken} if acquired, or {@code null}
     */
    public LockToken tryLock(String key, long leaseTime, TimeUnit timeUnit) {
        return tryLock(key, 0, leaseTime, timeUnit);
    }

    /**
     * Release a distributed lock. Only succeeds if the caller is the lock owner.
     *
     * @param token the lock token returned by {@link #tryLock}
     * @return {@code true} if the lock was successfully released
     */
    public boolean unlock(LockToken token) {
        if (token == null) return false;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script,
                Collections.singletonList(token.key()),
                token.owner());

        boolean released = result != null && result == 1L;
        if (released) {
            log.debug("Lock released: key={}, owner={}", token.key(), token.owner());
        } else {
            log.warn("Lock release failed (not owner or expired): key={}, owner={}",
                    token.key(), token.owner());
        }
        return released;
    }

    /**
     * Renew (extend) the TTL of a held lock without releasing it.
     * Useful for long-running operations that need the lock beyond the initial lease.
     *
     * @param token     the lock token
     * @param leaseTime new lease time from now
     * @param timeUnit  time unit
     * @return {@code true} if renewal succeeded
     */
    public boolean renew(LockToken token, long leaseTime, TimeUnit timeUnit) {
        if (token == null) return false;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RENEW_SCRIPT, Long.class);
        long leaseMillis = timeUnit.toMillis(leaseTime);
        Long result = redisTemplate.execute(script,
                Collections.singletonList(token.key()),
                token.owner(), String.valueOf(leaseMillis));

        boolean renewed = result != null && result == 1L;
        if (renewed) {
            log.debug("Lock renewed: key={}, newLeaseMs={}", token.key(), leaseMillis);
        } else {
            log.warn("Lock renewal failed: key={}", token.key());
        }
        return renewed;
    }

    /**
     * Check if a lock key is currently held by anyone.
     *
     * @param key lock key
     * @return {@code true} if the lock exists in Redis
     */
    public boolean isLocked(String key) {
        String lockKey = LOCK_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }

    /**
     * Immutable token returned on successful lock acquisition.
     * Must be passed to {@link #unlock} / {@link #renew} to prove ownership.
     */
    public record LockToken(String key, String owner, long leaseMillis) {}
}
