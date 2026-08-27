package com.example.url_shortener.domain.repository;

import com.example.url_shortener.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByVerificationToken(String verificationToken);

    /**
     * 'clearAutomatically = true' guarantees the local Hibernate cache gets cleared
     * so subsequent database reads don't return stale entity data.
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.verificationToken = null, u.tokenExpiresAt = null," +
            "u.enabled = true WHERE u.id = :userId")
    void activateUser(@Param("userId") UUID userId);

    /**
     * Attempt to activate user directly matching an unexpired token in one single database pass.
     * Returns the count of rows modified (will be 1 if token is valid and unexpired, 0 otherwise).
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User u SET u.verificationToken = null, u.tokenExpiresAt = null," +
            "u.enabled = true WHERE u.verificationToken = :token AND u.tokenExpiresAt > :now")
    int tryActivateWithUnexpiredToken(@Param("token") String token, @Param("now") Instant now);

    /**
     * Find users with expired tokens (for cleanup batch operations)
     */
    @Query("SELECT u from User u WHERE u.tokenExpiresAt is NOT NULL AND " +
            "u.tokenExpiresAt < :now")
    List<User> findUserWithExpiredTokens(@Param("now") Instant now);
}
