package com.example.url_shortener.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.example.url_shortener.domain.entity.Url;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class UrlDto{

    private UrlDto() {}

    // ── Requests ──
    public record ShortenRequest(
            @NotBlank(message = "URL must not be blank")
            @Size(max = 2048, message = "URL must not exceed 2048 characters")
            @Pattern(
                    regexp = "^https?://[\\w-]+(\\.[\\w-]+)+.*$",
                    message = "URL must be a valid fully-formed web address starting with http:// or https://"
            )
            String url,

            @Size(max = 30, message = "Custom alias must not exceed 30 characters")
            @Pattern(
                    regexp = "^[a-zA-Z0-9_-]*$",
                    message = "Custom alias may only contain letters, digits, hyphens, and underscores"
            )
            String customAlias,

            @Size(max = 255, message = "Title must not exceed 255 characters")
            String title,

            @Future(message = "Expiration timestamp must be a date validation in the future")
            Instant expiresAt
    ) {}

    public record UpdateRequest(
            @Size(max = 2048, message = "URL must not exceed 2048 characters")
            @Pattern(
                    regexp = "^$|^https?://[\\w-]+(\\.[\\w-]+)+.*$",
                    message = "URL must be a valid address starting with http:// or https://"
            )
            String url,

            @Size(max = 255, message = "Title must not exceed 255 characters")
            String title,

            Boolean active,

            @Future(message = "Expiration timestamp must be a date validation in the future")
            Instant expiresAt
    ) {}

    // ── Responses ──

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UrlResponse(
            UUID id,
            String shortCode,
            String shortUrl,
            String longUrl,
            String title,
            boolean active,
            long clickCount,
            Instant expiresAt,
            Instant createdAt
    ) {
        public static UrlResponse from(Url url, String baseUrl) {
            return new UrlResponse(
                    url.getId(),
                    url.getShortCode(),
                    baseUrl + "/" + url.getShortCode(),
                    url.getLongUrl(),
                    url.getTitle(),
                    url.isActive(),
                    url.getClickCount(),
                    url.getExpiresAt(),
                    url.getCreatedAt()
            );
        }
    }

    public record StatsResponse(
            UUID urlId,
            String shortCode,
            long totalClicks,
            List<DailyStat> dailyStats
    ) {}

    public record DailyStat(Instant day, long count) {}
}

