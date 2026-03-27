package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.auth.AuthResponse;
import com.example.SmartLearningPlatformBackend.dto.auth.LoginRequest;
import com.example.SmartLearningPlatformBackend.dto.auth.RegisterRequest;
import com.example.SmartLearningPlatformBackend.enums.UserRole;
import com.example.SmartLearningPlatformBackend.models.Student;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.repository.UserRepository;
import com.example.SmartLearningPlatformBackend.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;
        private final NotificationService notificationService;

        // ─── Register ────────────────────────────────────────────────────────────

        public AuthResponse register(RegisterRequest request) {
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new IllegalArgumentException("Email already in use");
                }

                String verificationToken = UUID.randomUUID().toString();

                var student = Student.builder()
                                .firstName(request.getFirstName())
                                .lastName(request.getLastName())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .role(UserRole.STUDENT)
                                .isActive(true)
                                .isVerified(false)
                                .verificationToken(verificationToken)
                                .lastVerificationEmailSent(LocalDateTime.now())
                                .dateOfBirth(request.getDateOfBirth())
                                .phoneNumber(request.getPhoneNumber())
                                .build();

                var saved = userRepository.save(student);

                String verificationLink = "http://localhost:4200/verify-email?token=" + verificationToken;
                notificationService.sendEmailNotification(
                                saved.getId(),
                                "Verify your LearnAI account",
                                "Click the button below to verify your account. This link does not expire.",
                                verificationLink);

                return AuthResponse.builder()
                                .id(saved.getId())
                                .firstName(saved.getFirstName())
                                .lastName(saved.getLastName())
                                .email(saved.getEmail())
                                .role(saved.getRole())
                                .build();
        }

        // ─── Login ───────────────────────────────────────────────────────────────

        public AuthResponse login(LoginRequest request) {
                var user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                if (Boolean.FALSE.equals(user.getIsVerified())) {
                        throw new RuntimeException("Account not verified. Please check your email.");
                }

                if (Boolean.FALSE.equals(user.getIsActive())) {
                        throw new RuntimeException("Account suspended. Please contact support.");
                }

                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);

                var userDetails = new UserDetailsImpl(user);
                var token = jwtService.generateToken(userDetails);

                return AuthResponse.builder()
                                .token(token)
                                .id(user.getId())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .email(user.getEmail())
                                .role(user.getRole())
                                .build();
        }

        // ─── Verify email ─────────────────────────────────────────────────────────

        @Transactional
        public void verifyEmail(String token) {
                var user = userRepository.findByVerificationToken(token)
                                .orElseThrow(() -> new RuntimeException("Invalid or expired verification token."));

                user.setIsVerified(true);
                user.setVerificationToken(null);
                userRepository.save(user);
        }

        // ─── Resend verification email ────────────────────────────────────────────

        @Transactional
        public void resendVerificationEmail(String email) {
                var userOpt = userRepository.findByEmail(email);
                if (userOpt.isEmpty()) {
                        // Do not leak whether the email exists (security best practice)
                        return;
                }
                var user = userOpt.get();

                if (Boolean.TRUE.equals(user.getIsVerified())) {
                        throw new RuntimeException("Account is already verified.");
                }

                // Rate-limit: 45 seconds between resend attempts
                if (user.getLastVerificationEmailSent() != null) {
                        long secondsSinceLast = java.time.Duration
                                        .between(user.getLastVerificationEmailSent(), LocalDateTime.now())
                                        .getSeconds();
                        if (secondsSinceLast < 45) {
                                throw new RuntimeException("Please wait before requesting another verification email.");
                        }
                }

                String verificationToken = UUID.randomUUID().toString();
                user.setVerificationToken(verificationToken);
                user.setLastVerificationEmailSent(LocalDateTime.now());
                userRepository.save(user);

                String verificationLink = "http://localhost:4200/verify-email?token=" + verificationToken;
                notificationService.sendEmailNotification(
                                user.getId(),
                                "Verify your LearnAI account",
                                "Click the button below to verify your account. This link does not expire.",
                                verificationLink);
        }

        // ─── Forgot password ──────────────────────────────────────────────────────

        @Transactional
        public void forgotPassword(String email) {
                var userOpt = userRepository.findByEmail(email);
                if (userOpt.isEmpty()) {
                        // Do not leak whether the email exists (security best practice)
                        return;
                }
                var user = userOpt.get();

                String resetToken = UUID.randomUUID().toString();
                user.setResetToken(resetToken);
                user.setResetTokenExpiry(LocalDateTime.now().plusHours(24));
                userRepository.save(user);

                String resetLink = "http://localhost:4200/reset-password?token=" + resetToken;
                notificationService.sendEmailNotification(
                                user.getId(),
                                "Reset your LearnAI password",
                                "Click the button below to reset your password. This link expires in 24 hours.",
                                resetLink);
        }

        // ─── Reset password ───────────────────────────────────────────────────────

        @Transactional
        public void resetPassword(String token, String newPassword) {
                var user = userRepository.findByResetToken(token)
                                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token."));

                if (user.getResetTokenExpiry() == null || LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
                        throw new RuntimeException("Reset link has expired.");
                }

                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetToken(null);
                user.setResetTokenExpiry(null);
                userRepository.save(user);
        }

        // ─── Validate reset token ─────────────────────────────────────────────────

        public void validateResetToken(String token) {
                var user = userRepository.findByResetToken(token)
                                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token."));

                if (user.getResetTokenExpiry() == null || LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
                        throw new RuntimeException("Reset link has expired.");
                }
        }
}
