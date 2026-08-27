package com.example.url_shortener.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RateLimitResult {
    private boolean consumed;
    private long remainingTokens;
    private long nanosWaitForRefill;
    private long nanosWaitForReset;
    private double retryAfterSeconds;
    private long tokensRequested;

    public String getRetryAfterHeader() {
        if (consumed) return null;
        return String.format("%.0f", Math.ceil(retryAfterSeconds));
    }

    public long getResetTimestamp() {
        if (consumed) return 0;
        return System.currentTimeMillis() + (nanosWaitForReset / 1_000_000);
    }
}
