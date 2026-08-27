package com.example.url_shortener.common.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    @Valid
    private RateLimitConfig auth = new RateLimitConfig();

    @Valid
    private RateLimitConfig urls = new RateLimitConfig();

    @Data
    public static class RateLimitConfig {

        private boolean enabled = true;

        @Min(1)
        private int capacity;

        @Valid
        private RefillConfig refill = new RefillConfig();

        private List<String> whitelistIps = new ArrayList<>();

        @Data
        public static class RefillConfig {
            @Min(1)
            private int tokens = 10;

            private Duration duration = Duration.ofMinutes(1);
        }
    }
}
