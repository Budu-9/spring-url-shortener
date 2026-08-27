package com.example.url_shortener.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

public final class AuthDto {

    private AuthDto() {}

    public record RegisterRequest(
            @NotBlank(message = "Email must not be empty")
            @Email(message = "Provide a valid email destination mapping configuration")
            String email,

            @NotBlank(message = "Password cannot be empty")
            @Size(min = 8, max = 100, message = "Password must span between 8 and 100 characters")
            String password
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Email must not be empty")
            @Email(message = "Provide a valid email destination mapping configuration")
            String email,

            @NotBlank(message = "Password field entry verification required")
            String password
    ) {}

    public record RefreshTokenRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}

    public record AuthResponse(
            String status,
            int statusCode,
            String accessTokens,
            String refreshToken,
            String email,
            String role,
            String message
    ) {}
}
