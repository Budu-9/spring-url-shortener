package com.example.url_shortener.common.ratelimit;

import com.example.url_shortener.common.ratelimit.RateLimitManager.RateLimitType;
import com.example.url_shortener.domain.dto.RateLimitResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitManager rateLimitManager;
    private final ObjectMapper objectMapper;


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();
        RateLimitType limitType = determineRateLimitType(path);

        if (rateLimitManager.isWhitelisted(clientIp, limitType)) {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitResult result = rateLimitManager.tryConsume(clientIp, limitType);

        setRateLimitHeaders(response, result);

        if(result.isConsumed()){
            filterChain.doFilter(request, response);
        } else {
            handleRateLimitExceeded(response, request, result, limitType);
        }
    }

    private void setRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemainingTokens()));
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.getTokensRequested()));

        if(!result.isConsumed()) {
            response.setHeader("Retry-After", result.getRetryAfterHeader());
            response.setHeader("X-RateLimit-Limit", String.valueOf(result.getResetTimestamp()));
        }
    }

    private void handleRateLimitExceeded(HttpServletResponse response,
                                         HttpServletRequest request,
                                         RateLimitResult result,
                                         RateLimitType type) {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");

        Map<String, Object> errorResponse = Map.of(
                "error", "Too Many Requests",
                "message", "Rate limit exceeded. Please try again later",
                "retryAfterSeconds", result.getRetryAfterSeconds(),
                "type", type.name(),
                "timestamp", System.currentTimeMillis()
        );

        try {
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        } catch (IOException e) {
            log.error("Error writing rate limit response", e);
        }

        log.warn("Rate limit exceeded - IP: {}, Type: {}, RetryAfter: {}s",
                getClientIp(request), type.name(), result.getRetryAfterSeconds());
    }

    private RateLimitType determineRateLimitType(String path) {
        return switch (getPathPrefix(path)) {
            case "/api/auth/" -> RateLimitType.AUTH;
            case "/api/urls/", "/api/shorten/" -> RateLimitType.URLS;
            default -> null;
        };
    }

    private String getPathPrefix(String path) {
        if (path.startsWith("/api/auth/")) return "/api/auth/";
        if (path.startsWith("/api/urls/")) return "/api/urls/";
        if (path.startsWith("/api/shorten/")) return "/api/shorten/";
        return "";
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
