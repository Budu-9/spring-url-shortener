package com.example.url_shortener.common.event;

import com.example.url_shortener.common.exception.EmailDeliveryException;
import com.example.url_shortener.domain.dto.EmailMessage;
import com.example.url_shortener.infrastructure.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for domain events and dispatches transactional emails asynchronously.
 * Each handler runs on the "emailExecutor" thread pool (defined in AsyncConfig),
 * so the HTTP response is never blocked waiting for SMTP.
 * Add new @EventListener methods here as the feature set grows
 * (password reset, URL expiry warnings, etc.).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventListener {
    private final EmailService emailService;

    // ── Registration verification email ──
    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent userRegisteredEvent) {
        String email = userRegisteredEvent.getUser().getEmail();
        log.info("Sending verification email to={}", email);

        try {
            EmailMessage message = emailService.buildVerificationEmail(userRegisteredEvent.getUser());
            emailService.send(message);
        } catch (EmailDeliveryException ex) {
            // Log the failure but don't rethrow — the user is already registered.
            // Option to push to a dead-letter queue or retry via a scheduler(not implemented yet).
            log.error("Verification email delivery failed for user={}: {}", email, ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error sending verification email to={}: {}", email, ex.getMessage(), ex);
        }
    }

    // ── Registration welcome email ──
    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserVerified(UserVerifiedEvent userVerifiedEvent) {
        String email = userVerifiedEvent.getUser().getEmail();
        log.info("Sending welcome email to={}", email);

        try {
            EmailMessage message = emailService.buildWelcomeEmail(userVerifiedEvent.getUser());
            emailService.send(message);
        } catch (EmailDeliveryException ex) {
            // Log the failure but don't rethrow — the user is already verified.
            // Option to push to a dead-letter queue or retry via a scheduler(not implemented yet)
            log.error("Welcome email delivery failed for user={}: {}", email, ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error sending welcome email to={}: {}", email, ex.getMessage(), ex);
        }
    }
}
