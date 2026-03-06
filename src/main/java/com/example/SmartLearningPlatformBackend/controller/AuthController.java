package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.auth.AuthResponse;
import com.example.SmartLearningPlatformBackend.dto.auth.ForgotPasswordRequest;
import com.example.SmartLearningPlatformBackend.dto.auth.LoginRequest;
import com.example.SmartLearningPlatformBackend.dto.auth.RegisterRequest;
import com.example.SmartLearningPlatformBackend.dto.auth.ResendVerificationRequest;
import com.example.SmartLearningPlatformBackend.dto.auth.ResetPasswordRequest;
import com.example.SmartLearningPlatformBackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ─── Register ─────────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Surface "Account not verified" as 403, all other auth errors as 401
            if (e.getMessage() != null && e.getMessage().contains("not verified")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }
    }

    // ─── Verify email ─────────────────────────────────────────────────────────

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            authService.verifyEmail(token);
            return ResponseEntity.ok(Map.of("message", "Email verified successfully. You can now log in."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ─── Resend verification email ────────────────────────────────────────────

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        try {
            authService.resendVerificationEmail(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "Verification email sent. Please check your inbox."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ─── Forgot password ──────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.forgotPassword(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "Password reset email sent. Please check your inbox."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // ─── Reset password ───────────────────────────────────────────────────────

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now log in."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
