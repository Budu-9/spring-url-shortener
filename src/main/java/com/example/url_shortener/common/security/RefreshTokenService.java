package com.example.url_shortener.common.security;

import com.example.url_shortener.common.exception.InvalidTokenException;
import com.example.url_shortener.domain.entity.RefreshToken;
import com.example.url_shortener.domain.entity.User;
import com.example.url_shortener.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.jwt.refresh-expiration-ms:7d}")
    private Duration refreshExpiration;

    @Transactional
    public String createRefreshToken(User user, String ipAddress, String device) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken= HexFormat.of().formatHex(randomBytes);

        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(Instant.now().plus(refreshExpiration))
                .ipAddress(ipAddress)
                .device(device)
                .build();
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public User verifyAndConsumeToken(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        if(refreshToken.isRevoked()) {
            refreshTokenRepository.revokeAllUserToken(refreshToken.getUser());
            throw new InvalidTokenException("Refresh token has been revoked.");
        }
        if(refreshToken.isExpired()) {
            throw new InvalidTokenException("Refresh token has expired. Please log in again.");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return refreshToken.getUser();
    }

    @Transactional
    public void revokeToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private String hashToken(String rawToken) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
