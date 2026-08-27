package com.example.url_shortener.domain.controller;

import com.example.url_shortener.domain.dto.AuthDto.RefreshTokenRequest;
import com.example.url_shortener.domain.dto.AuthDto.LoginRequest;
import com.example.url_shortener.domain.dto.AuthDto.RegisterRequest;
import com.example.url_shortener.domain.dto.AuthDto.AuthResponse;
import com.example.url_shortener.domain.entity.User;
import com.example.url_shortener.domain.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify user account")
    public ResponseEntity<AuthResponse> verifyAndActivateUser(
            @RequestParam("token") String token,
            HttpServletRequest request
    ) {
        String ipAddress = extractClientIp(request);
        String device = extractUserAgent(request);

        AuthResponse response = authService.verifyAndActivateUser(token, ipAddress, device);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "User login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = extractClientIp(httpRequest);
        String device = extractUserAgent(httpRequest);

        AuthResponse response = authService.login(request, ipAddress, device);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Verify refresh token and issue new tokens (access and refresh) ")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = extractClientIp(httpRequest);
        String device = extractUserAgent(httpRequest);

        AuthResponse response = authService.refreshToken(request.refreshToken(), ipAddress, device);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "User logout on all devices and revoke all active refresh tokens")
    public ResponseEntity<Void> logOutAllDevices(@AuthenticationPrincipal User user) {
        authService.logOutAllDevices(user);
        return ResponseEntity.noContent().build();
    }


    // private methods
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return (userAgent != null && !userAgent.isBlank()) ? userAgent : "Unknown Device";
    }
}
