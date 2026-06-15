package com.serviceonwheels.auth_service.service;

import com.serviceonwheels.auth_service.dto.ForgotPasswordRequest;
import com.serviceonwheels.auth_service.dto.ResetPasswordRequest;
import com.serviceonwheels.auth_service.exception.InvalidResetTokenException;
import com.serviceonwheels.auth_service.model.PasswordResetToken;
import com.serviceonwheels.auth_service.model.User;
import com.serviceonwheels.auth_service.repository.PasswordResetTokenRepository;
import com.serviceonwheels.auth_service.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUpMailSender() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        lenient().when(mailSender.createMimeMessage()).thenReturn(message);
    }

    // ── Forgot Password ──────────────────────────────────────────

    @Nested
    @DisplayName("Forgot Password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("returns generic message even when user exists")
        void returnsGenericMessage_whenUserExists() {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("test@example.com");

            User user = User.builder().id("u1").email("test@example.com").build();
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, String> result = passwordResetService.handleForgotPassword(req);

            assertEquals("If the account exists, reset instructions have been sent.", result.get("message"));
            verify(tokenRepository).deleteAllByEmail("test@example.com");
            verify(tokenRepository).save(any(PasswordResetToken.class));
        }

        @Test
        @DisplayName("returns generic message when user does NOT exist — no token created")
        void returnsGenericMessage_whenUserNotFound() {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("unknown@example.com");

            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            Map<String, String> result = passwordResetService.handleForgotPassword(req);

            assertEquals("If the account exists, reset instructions have been sent.", result.get("message"));
            verify(tokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("generated token has 30-min expiry")
        void tokenHasCorrectExpiry() {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("test@example.com");

            User user = User.builder().id("u1").email("test@example.com").build();
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            passwordResetService.handleForgotPassword(req);

            ArgumentCaptor<PasswordResetToken> cap = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(cap.capture());

            PasswordResetToken saved = cap.getValue();
            assertNotNull(saved.getTokenHash());
            assertFalse(saved.isUsed());
            // Expiry should be ~30 minutes from now
            assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(29)));
            assertTrue(saved.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(31)));
        }
    }

    // ── Reset Password ───────────────────────────────────────────

    @Nested
    @DisplayName("Reset Password")
    class ResetPasswordTests {

        @Test
        @DisplayName("successfully resets password with valid token")
        void resetsPassword_whenTokenValid() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .tokenHash("hashed-valid-token")
                    .email("test@example.com")
                    .used(false)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();

            User user = User.builder().id("u1").email("test@example.com").password("old-hash").build();

            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("NewPassword1!")).thenReturn("new-hash");

            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setToken("valid-token");
            req.setPassword("NewPassword1!");

            Map<String, String> result = passwordResetService.handleResetPassword(req);

            assertTrue(result.get("message").contains("successfully"));
            verify(passwordEncoder).encode("NewPassword1!");
            verify(userRepository).save(user);
            assertTrue(token.isUsed());
        }

        @Test
        @DisplayName("rejects expired token")
        void rejectsExpiredToken() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .tokenHash("hashed-expired-token")
                    .email("test@example.com")
                    .used(false)
                    .expiresAt(LocalDateTime.now().minusMinutes(5))
                    .build();

            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setToken("expired-token");
            req.setPassword("NewPassword1!");

            InvalidResetTokenException ex = assertThrows(
                    InvalidResetTokenException.class,
                    () -> passwordResetService.handleResetPassword(req));

            assertTrue(ex.getMessage().contains("expired"));
        }

        @Test
        @DisplayName("rejects already-used token")
        void rejectsUsedToken() {
            PasswordResetToken token = PasswordResetToken.builder()
                    .tokenHash("hashed-used-token")
                    .email("test@example.com")
                    .used(true)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();

            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setToken("used-token");
            req.setPassword("NewPassword1!");

            InvalidResetTokenException ex = assertThrows(
                    InvalidResetTokenException.class,
                    () -> passwordResetService.handleResetPassword(req));

            assertTrue(ex.getMessage().contains("already been used"));
        }

        @Test
        @DisplayName("rejects invalid token")
        void rejectsInvalidToken() {
            when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setToken("bad-token");
            req.setPassword("NewPassword1!");

            assertThrows(InvalidResetTokenException.class,
                    () -> passwordResetService.handleResetPassword(req));
        }
    }
}
