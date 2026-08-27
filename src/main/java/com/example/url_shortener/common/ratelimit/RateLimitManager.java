package com.example.url_shortener.common.ratelimit;

import com.example.url_shortener.domain.dto.BucketInfo;
import com.example.url_shortener.domain.dto.RateLimitResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.example.url_shortener.common.ratelimit.RateLimitProperties.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitManager {
    private final RateLimitProperties properties;

    private final Map<RateLimitType, Cache<String, Bucket>> caches = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Initialize caches for each rate limit type
        for (RateLimitType type: RateLimitType.values()) {
            RateLimitConfig config = getConfig(type);
            caches.put(type, createTimeBasedCache(config.getRefill().getDuration()));
        }
        log.info("Rate limiter caches for initialized for {} types", caches.size());
    }

    /**
     * Create a Caffeine cache with appropriate eviction policy
     */
    private Cache <String, Bucket> createTimeBasedCache(Duration windowSize) {
        return Caffeine.newBuilder()
                .expireAfterAccess(windowSize.multipliedBy(2))
                .maximumSize(50_000)
                .recordStats()
                .scheduler(Scheduler.systemScheduler())
                .removalListener((key, value, cause) ->
                        log.debug("Bucket evicted: key={}, cause={}", key, cause))
                .build();
    }

    /**
     * Try to consume a token and get detailed result
     * This is the PRIMARY method - replaces multiple calls with one
     */
    public RateLimitResult tryConsume(String key, RateLimitType type) {
        if(type == null) {
            return RateLimitResult.builder()
                    .consumed(true)
                    .remainingTokens(Long.MAX_VALUE)
                    .build();
        }

        Bucket bucket = resolveBucket(key, type);

        // Single atomic operation that returns rich metadata
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        return RateLimitResult.builder()
                .consumed(probe.isConsumed())
                .remainingTokens(probe.getRemainingTokens())
                .nanosWaitForRefill(probe.getNanosToWaitForRefill())
                .nanosWaitForReset(probe.getNanosToWaitForReset())
                .retryAfterSeconds(probe.getNanosToWaitForRefill() / 1_000_000_000.0)
                .build();
    }

    /**
     * Try to consume multiple tokens
     */
    public RateLimitResult tryConsumeMultiple(String key, RateLimitType type, long tokens) {
        Bucket bucket = resolveBucket(key, type);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(tokens);

        return RateLimitResult.builder()
                .consumed(probe.isConsumed())
                .remainingTokens(probe.getRemainingTokens())
                .nanosWaitForRefill(probe.getNanosToWaitForRefill())
                .nanosWaitForReset(probe.getNanosToWaitForReset())
                .retryAfterSeconds(probe.getNanosToWaitForRefill())
                .tokensRequested(tokens)
                .build();
    }

    /**
     * Get bucket information without consuming
     */
    public BucketInfo getBucketInfo(String key, RateLimitType type) {
        Bucket bucket = resolveBucket(key, type);
        RateLimitConfig config = getConfig(type);

        long availableTokens = bucket.getAvailableTokens();
        long consumedTokens = config.getCapacity() - availableTokens;

        return BucketInfo.builder()
                .availableTokens(availableTokens)
                .consumedTokens(Math.max(0, consumedTokens))
                .build();
    }

    /**
     * Resolve or create bucket for a key and rate limit type
     */
    private Bucket resolveBucket(String key, RateLimitType type) {
        Cache<String, Bucket> cache = caches.get(type);

        if(cache == null) {
            throw new IllegalArgumentException("Unknown rate limit type:" + type);
        }

        return cache.get(key, k -> {
            log.debug("Creating new bucket for key: {}, type: {}", k, type);
            return createBucket(type);
        });
    }

    /**
     * Create a new bucket based on configuration
     */
    private Bucket createBucket (RateLimitType type) {
        RateLimitConfig config = getConfig(type);

        if(!config.isEnabled()) {
            return Bucket.builder()
                    .addLimit(limit -> limit
                            .capacity(Long.MAX_VALUE)
                            .refillIntervally(Long.MAX_VALUE, Duration.ofDays(365))
                    )
                    .build();
        }

        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(config.getCapacity())
                        .refillIntervally(
                                config.getRefill().getTokens(),
                                config.getRefill().getDuration()
                        )
                )
                .build();
    }

    /**
     * Check if IP is whitelisted
     */
    public boolean isWhitelisted(String clientIp, RateLimitType type) {
        if (type == null) {
            return true;
        }

        RateLimitConfig config = getConfig(type);
        return config.getWhitelistIps() != null &&
                (config.getWhitelistIps().contains(clientIp) ||
                        config.getWhitelistIps().contains("0.0.0.0/0"));
    }

    /**
     * Get configuration for rate limit type
     */
    private RateLimitConfig getConfig(RateLimitType type) {
        if(type == null) {
            return getDefaultConfig();
        }
        return switch (type) {
            case URLS -> properties.getUrls();
            case AUTH -> properties.getAuth();
        };
    }

    private RateLimitConfig getDefaultConfig() {
        RateLimitConfig config = new RateLimitConfig();
        config.setEnabled(true);
        config.setCapacity(100);

        return config;
    }

    /**
     * Get cache statistics for monitoring
     */
    public Map<String, Object> getStatistics(RateLimitType type) {
        Cache<String, Bucket> cache = caches.get(type);
        if(cache == null) return Map.of();

        return Map.of(
                "size", cache.estimatedSize(),
                "hitCount", cache.stats().hitCount(),
                "missCount", cache.stats().missCount(),
                "hitRate", String.format("%.2f%%", cache.stats().hitRate() * 100),
                "evictionCount", cache.stats().evictionCount()
        );
    }

    /**
     * Manually evict a specific key
     */
    public void evict(String key, RateLimitType type) {
        Cache<String, Bucket> cache = caches.get(type);
        if (cache != null) {
            cache.invalidate(key);
            log.debug("Manually evicted key: {}, type: {}", key, type);
        }
    }

    /**
     * Clear all caches (useful for testing)
     */
    public void clearAll() {
        caches.values().forEach(Cache::invalidateAll);
        log.info("All rate limiter caches cleared");
    }

    public enum RateLimitType {
        URLS, AUTH
    }
}
