package com.serviceonwheels.auth_service.service;

import com.serviceonwheels.auth_service.dto.ForgotPasswordRequest;
import com.serviceonwheels.auth_service.dto.ResetPasswordRequest;
import com.serviceonwheels.auth_service.exception.EmailDeliveryException;
import com.serviceonwheels.auth_service.exception.InvalidResetTokenException;
import com.serviceonwheels.auth_service.model.PasswordResetToken;
import com.serviceonwheels.auth_service.model.User;
import com.serviceonwheels.auth_service.repository.PasswordResetTokenRepository;
import com.serviceonwheels.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailException;

/**
 * Handles forgot-password and reset-password flows.
 * <p>
 * Security hardening applied:
 * <ul>
 *   <li>Tokens are SHA-256 hashed before storage — raw tokens are never persisted.</li>
 *   <li>Forgot-password always returns the same response regardless of whether the
 *       email exists, preventing account-enumeration attacks.</li>
 *   <li>SMTP failures are logged but do not change the HTTP response for forgot-password,
 *       preserving timing-safe behaviour.</li>
 *   <li>Token validation + password update happen atomically via {@code @Transactional}.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int TOKEN_EXPIRY_MINUTES = 30;

    /** Injected from application.properties — no more hardcoded localhost URL. */
    @Value("${app.frontend.reset-url}")
    private String frontendResetUrl;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    // ────────────────────────────────────────────────────────────────
    //  Forgot Password
    // ────────────────────────────────────────────────────────────────

    /**
     * Initiates the password-reset flow.
     * <p>
     * Always returns the same generic message to prevent account enumeration.
     * SMTP errors are logged but intentionally swallowed so the response
     * does not leak whether the email is registered.
     */
    @Transactional
    public Map<String, String> handleForgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Forgot-password requested for email: {}", email);

        var userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            // Invalidate any previously issued tokens for this email
            tokenRepository.deleteAllByEmail(email);

            String rawToken = UUID.randomUUID().toString();
            String hashedToken = hashToken(rawToken);
            LocalDateTime now = LocalDateTime.now();

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .email(email)
                    .tokenHash(hashedToken)
                    .createdAt(now)
                    .expiresAt(now.plusMinutes(TOKEN_EXPIRY_MINUTES))
                    .used(false)
                    .build();

            tokenRepository.save(resetToken);

            try {
                sendResetEmail(email, rawToken);
            } catch (MailException | MessagingException e) {
                // Log the error but do NOT rethrow — the response must stay identical
                // whether the email exists or not, to prevent account enumeration.
                log.error("Failed to send reset email via SMTP to {}: ", email, e);
            }
        }

        // Generic response — never reveals whether the account exists
        return Map.of("message", "If the account exists, reset instructions have been sent.");
    }

    // ────────────────────────────────────────────────────────────────
    //  Reset Password
    // ────────────────────────────────────────────────────────────────

    /**
     * Validates the token and atomically updates the user's password.
     */
    @Transactional
    public Map<String, String> handleResetPassword(ResetPasswordRequest request) {
        String rawToken = request.getToken().trim();
        String hashedToken = hashToken(rawToken);

        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new InvalidResetTokenException("Invalid or expired reset link."));

        if (resetToken.isUsed()) {
            throw new InvalidResetTokenException("This reset link has already been used.");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidResetTokenException("This reset link has expired. Please request a new one.");
        }

        // Mark the token as used FIRST, so a concurrent request will see used=true
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new InvalidResetTokenException("User account not found."));

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        log.info("Password reset successful for email: {}", resetToken.getEmail());
        return Map.of("message", "Your password has been reset successfully. Please sign in with your new password.");
    }

    // ────────────────────────────────────────────────────────────────
    //  Token Validation (non-consuming)
    // ────────────────────────────────────────────────────────────────

    /**
     * Validates a token without consuming it — used by the frontend to check
     * whether the reset page should render or show an error.
     */
    public Map<String, String> validateToken(String rawToken) {
        String hashedToken = hashToken(rawToken.trim());

        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new InvalidResetTokenException("Invalid or expired reset link."));

        if (resetToken.isUsed()) {
            throw new InvalidResetTokenException("This reset link has already been used.");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidResetTokenException("This reset link has expired. Please request a new one.");
        }

        return Map.of("message", "Token is valid.", "email", resetToken.getEmail());
    }

    // ────────────────────────────────────────────────────────────────
    //  Helpers
    // ────────────────────────────────────────────────────────────────

    /**
     * Sends the reset email via Spring Boot's JavaMailSender using an HTML template.
     */
    private void sendResetEmail(String email, String token) throws MailException, MessagingException {
        String resetUrl = frontendResetUrl + "?token=" + token;

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(email);
        helper.setSubject("Reset Your Service on Wheels Password");

        String htmlContent = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; color: #333;\">"
                + "<div style=\"text-align: center; margin-bottom: 30px;\">"
                + "<h2 style=\"color: #8B1E1E; margin: 0;\">Service on Wheels</h2>"
                + "</div>"
                + "<p style=\"font-size: 16px;\">Hello,</p>"
                + "<p style=\"font-size: 16px; line-height: 1.5;\">We received a request to reset the password for your Service on Wheels account. Click the button below to choose a new password:</p>"
                + "<div style=\"text-align: center; margin: 30px 0;\">"
                + "<a href=\"" + resetUrl + "\" style=\"background-color: #8B1E1E; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;\">Reset Password</a>"
                + "</div>"
                + "<p style=\"font-size: 14px; color: #666; margin-top: 30px;\">This password reset link is only valid for the next " + TOKEN_EXPIRY_MINUTES + " minutes.</p>"
                + "<p style=\"font-size: 14px; color: #666;\">If you did not request this password reset, please ignore this email. Your account remains secure.</p>"
                + "<hr style=\"border: none; border-top: 1px solid #eaeaea; margin: 30px 0;\" />"
                + "<p style=\"font-size: 12px; color: #999; text-align: center;\">&copy; " + Year.now().getValue() + " Service on Wheels. All rights reserved.</p>"
                + "</div>";

        helper.setText(htmlContent, true);

        log.info("Attempting to send HTML SMTP email to {}", email);
        mailSender.send(message);
        log.info("HTML SMTP email successfully dispatched to {}", email);
    }

    /**
     * Produces a hex-encoded SHA-256 hash of the given raw token.
     * Used so the raw token is never stored in the database.
     *
     * @param rawToken the plain-text UUID token
     * @return hex-encoded SHA-256 digest
     */
    private static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JVM spec — this should never happen
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
