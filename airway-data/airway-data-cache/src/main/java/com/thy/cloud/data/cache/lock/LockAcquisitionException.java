package com.thy.cloud.data.cache.lock;

/**
 * Exception thrown when a distributed lock cannot be acquired
 * within the configured wait time.
 *
 * @author Engin Mahmut
 */
public class LockAcquisitionException extends RuntimeException {

    private final String lockKey;

    public LockAcquisitionException(String lockKey, String message) {
        super(message);
        this.lockKey = lockKey;
    }

    public LockAcquisitionException(String lockKey, String message, Throwable cause) {
        super(message, cause);
        this.lockKey = lockKey;
    }

    public String getLockKey() {
        return lockKey;
    }
}
