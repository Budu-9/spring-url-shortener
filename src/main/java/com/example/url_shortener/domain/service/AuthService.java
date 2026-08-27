package com.example.url_shortener.domain.service;

import com.example.url_shortener.common.event.UserRegisteredEvent;
import com.example.url_shortener.common.event.UserVerifiedEvent;
import com.example.url_shortener.common.exception.AccountAlreadyVerifiedException;
import com.example.url_shortener.common.exception.InvalidTokenException;
import com.example.url_shortener.common.security.RefreshTokenService;
import com.example.url_shortener.domain.dto.AuthDto.AuthResponse;
import com.example.url_shortener.domain.dto.AuthDto.LoginRequest;
import com.example.url_shortener.domain.dto.AuthDto.RegisterRequest;
import com.example.url_shortener.domain.entity.User;
import com.example.url_shortener.common.exception.EmailAlreadyExistsException;
import com.example.url_shortener.domain.repository.RefreshTokenRepository;
import com.example.url_shortener.domain.repository.UserRepository;
import com.example.url_shortener.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthResponse register(final RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("An account with this email address already exists.");
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();
        user = userRepository.save(user);

        String verificationToken = jwtService.generateVerificationToken(user);
        Instant tokenExpiresAt = jwtService.extractExpiration(verificationToken).toInstant();

        user.setVerificationToken(verificationToken);
        user.setTokenExpiresAt(tokenExpiresAt);

        userRepository.save(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(this, user));

        return new AuthResponse(
                "success",
                201,
                null,
                null,
                user.getEmail(),
                user.getRole(),
                "A verification email has been sent to your email address. Please check your inbox to activate your account."
        );
    }

    @Transactional
    public AuthResponse verifyAndActivateUser(final String token, String ipAddress, String device) {
        if(!jwtService.isVerificationTokenValid(token)) {
            throw new InvalidTokenException("Invalid verification token");
        }

        String email = jwtService.extractEmail(token);

        int activated = userRepository.tryActivateWithUnexpiredToken(token, Instant.now());

        if(activated == 0) {
            if (userRepository.findByEmail(email).map(User::isEnabled).orElse(false)) {
                throw new AccountAlreadyVerifiedException("Account is already verified. Please log in");
            }
            log.warn("Failed activation attempt for email: {}", email);
            throw new InvalidTokenException("Invalid or expired verification token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after activation"));

        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user, ipAddress, device);

        eventPublisher.publishEvent(new UserVerifiedEvent(this, user));

        return new AuthResponse(
                "success",
                200,
                accessToken,
                refreshToken,
                user.getEmail(),
                user.getRole(),
                "Your account has been verified. You can now login and enjoy full access"
        );
    }

    @Transactional
    public AuthResponse login(final LoginRequest request, String ipAddress, String device) {

        final User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials provided"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials provided");
        }

        if(!user.isEnabled()) {
            throw new DisabledException("Account is not verified. Please check your mail to activate your account");
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user, ipAddress, device);


        return new AuthResponse(
                "success",
                200,
                accessToken,
                refreshToken,
                user.getEmail(),
                user.getRole(),
                "Login successful"
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeToken(rawRefreshToken);
    }

    @Transactional
    public void logOutAllDevices(User user) {
        refreshTokenRepository.revokeAllUserToken(user);
    }


    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken, String ipAddress, String device) {
        User user = refreshTokenService.verifyAndConsumeToken(rawRefreshToken);

        String newAccessToken = jwtService.generateToken(user);
        String newRefreshToken = refreshTokenService.createRefreshToken(user, ipAddress, device);

        return new AuthResponse(
                "success",
                200,
                newAccessToken,
                newRefreshToken,
                user.getEmail(),
                user.getRole(),
                "Refresh token generated successfully"
        );
    }
}