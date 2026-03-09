package com.thy.cloud.service.api.modules.policy.service;

import com.thy.cloud.data.cache.util.CacheKeyPattern;

/**
 * Cache key definitions for the Policy module.
 * Uses the project's CacheKeyPattern convention.
 */
public enum PolicyCacheKey implements CacheKeyPattern {

    /**
     * Cache for resolved policy constraints.
     * Key pattern: policy:constraints:{scopeType}:{scopeKey}
     */
    POLICY_CONSTRAINTS("policy:constraints");

    private final String prefix;

    PolicyCacheKey(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    /** Spring @Cacheable cache name */
    public static final String CACHE_NAME = "policy:constraints";
}
