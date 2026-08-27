package com.example.url_shortener.infrastructure.email;

import com.example.url_shortener.domain.dto.EmailMessage;
import com.example.url_shortener.domain.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.base-url}")
    private String apiBaseUrl;

    public void send(EmailMessage message) {
        try{
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.textBody(), message.htmlBody());

            if (message.cc() != null && !message.cc().isEmpty()) {
                helper.setCc(message.cc().toArray(new String[0]));
            }
            if (message.bcc() != null && !message.bcc().isEmpty()) {
                helper.setBcc(message.bcc().toArray(new String[0]));
            }

            mailSender.send(mime);
            log.info("Email sent successfully to={} subject='{}'", message.to(), message.subject());
        } catch (MessagingException | MailException | UnsupportedEncodingException ex) {
            log.error("Failed to send email to={} subject='{}': {}", message.to(), message.subject(), ex.getMessage());
        }
    }

    // ── Email Template Builders ──

    public EmailMessage buildVerificationEmail(User user) {
        final String email  = user.getEmail();
        final String name = nameFrom(email);
        final String verificationUrl = apiBaseUrl + "/api/auth/verify?token=" + user.getVerificationToken();

        String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Verify Your Account</title>
            </head>
            <body style="margin:0;padding:0;background:#f4f4f5;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f5;padding:40px 0;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;box-shadow:0 1px 3px rgba(0,0,0,.08);">
                    <tr>
                      <td style="background:#4f46e5;padding:32px 40px;border-top-left-radius:8px;border-top-right-radius:8px;">
                        <h1 style="margin:0;font-size:22px;color:#ffffff;font-weight:600;">URLShortener</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:40px;">
                        <h2 style="margin:0 0 16px;font-size:18px;color:#111827;">Hello %s,</h2>
                        <p style="margin:0 0 16px;font-size:15px;color:#374151;line-height:1.6;">
                          Thank you for registering! Please verify your email address to fully activate your URLShortener account. This link expires in 24 hours.
                        </p>
                        <table cellpadding="0" cellspacing="0" style="margin:28px 0;">
                          <tr>
                            <td style="background:#4f46e5;border-radius:6px;padding:12px 28px;">
                              <a href="%s" style="color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;display:inline-block;">
                                Verify Email Address
                              </a>
                            </td>
                          </tr>
                        </table>
                        <p style="margin:0;font-size:13px;color:#9ca3af;">
                          If you did not sign up for this account, you can safely ignore this email.
                        </p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(name, verificationUrl);

        String text = """
            Hello %s,

            Thank you for registering! Please verify your email address to activate your account using the link below:
            %s

            This link will expire in 24 hours.
            """.formatted(name, verificationUrl);

        return EmailMessage.builder()
                .to(email)
                .subject("Action Required: Verify Your URLShortener Account")
                .htmlBody(html)
                .textBody(text)
                .build();
    }

    public EmailMessage buildWelcomeEmail(User user) {
        final String email = user .getEmail();
        final String name = nameFrom(email);
        final String dashboardUrl = apiBaseUrl + "/swagger-ui.html"; // Adjust to frontend dashboard route

        String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Welcome to URLShortener</title>
            </head>
            <body style="margin:0;padding:0;background:#f4f4f5;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f5;padding:40px 0;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;box-shadow:0 1px 3px rgba(0,0,0,.08);">
                    <tr>
                      <td style="background:#10b981;padding:32px 40px;border-top-left-radius:8px;border-top-right-radius:8px;">
                        <h1 style="margin:0;font-size:22px;color:#ffffff;font-weight:600;">Account Activated!</h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:40px;">
                        <h2 style="margin:0 0 16px;font-size:18px;color:#111827;">Welcome, %s 👋</h2>
                        <p style="margin:0 0 16px;font-size:15px;color:#374151;line-height:1.6;">
                          Your email verification is complete, and your account is officially active. You can now start shortening URLs, creating custom aliases, and evaluating click analytics.
                        </p>
                        <table cellpadding="0" cellspacing="0" style="margin:28px 0;">
                          <tr>
                            <td style="background:#10b981;border-radius:6px;padding:12px 28px;">
                              <a href="%s" style="color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;display:inline-block;">
                                Go to Dashboard →
                              </a>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(name, dashboardUrl);

        String text = """
            Welcome, %s!

            Your account has been fully activated. You can now start using your dashboard:
            %s
            """.formatted(name, dashboardUrl);

        return EmailMessage.builder()
                .to(email)
                .subject("Welcome to URLShortener — Account Active!")
                .htmlBody(html)
                .textBody(text)
                .build();
    }

    // ── Helpers ──

    private String nameFrom(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
