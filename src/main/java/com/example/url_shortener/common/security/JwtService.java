package com.example.url_shortener.common.security;

import com.example.url_shortener.common.exception.InvalidRefreshTokenException;
import com.example.url_shortener.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateVerificationToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("type", "VERIFICATION")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(signingKey)
                .compact();
    }

    public String generateToken(User user) {
        final Date now = new Date();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("type", "ACCESS")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = parseClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractClaim(String token, String claimName) {
        final Claims claims = parseClaims(token);
        Object claim = claims.get(claimName);
        return claim != null ? claim.toString() : null;
    }

    public Map<String, Object> extractAllClaims(String token) {
        return parseClaims(token);
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractTokenId(String token) {
        return parseClaims(token).getId();
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean isVerificationToken(String token) {
        Claims claims = parseClaims(token);
        return "VERIFICATION".equals(claims.get("type"));
    }

    /**
     * Exceptions are left to propagate out intentionally so your JwtAuthFilter
     * can explicitly catch them (e.g. ExpiredJwtException) and format a custom 401 response.
     */
    public boolean isValid(String token, UserDetails userDetails) {
        try {
            final Claims claims = parseClaims(token);
            final String email = claims.getSubject();

            if (!"ACCESS".equals(claims.get("type"))) {
                return false;
            }

            return email.equals(userDetails.getUsername()) && isClaimsExpired(claims);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isVerificationTokenValid(String token) {
        try{
            final Claims claims = parseClaims(token);
            return "VERIFICATION".equals(claims.get("type")) && !isClaimsExpired(claims);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isClaimsExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
