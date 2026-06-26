package com.example.SmartLearningPlatformBackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.SmartLearningPlatformBackend.dto.auth.AuthResponse;
import com.example.SmartLearningPlatformBackend.dto.auth.LoginRequest;
import com.example.SmartLearningPlatformBackend.dto.auth.RegisterRequest;
import com.example.SmartLearningPlatformBackend.enums.UserRole;
import com.example.SmartLearningPlatformBackend.models.Student;
import com.example.SmartLearningPlatformBackend.models.User;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.repository.UserRepository;
import com.example.SmartLearningPlatformBackend.security.jwt.JwtService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_createsUnverifiedStudentAndSendsVerificationEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        request.setEmail("ada@example.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student student = invocation.getArgument(0);
            student.setId(42L);
            return student;
        });

        AuthResponse response = authService.register(request);

        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(userRepository).save(studentCaptor.capture());
        Student savedStudent = studentCaptor.getValue();

        assertEquals(42L, response.getId());
        assertEquals(UserRole.STUDENT, savedStudent.getRole());
        assertTrue(savedStudent.getIsActive());
        assertFalse(savedStudent.getIsVerified());
        assertEquals("encoded-password", savedStudent.getPassword());
        assertNotNull(savedStudent.getVerificationToken());
        assertNotNull(savedStudent.getVerificationTokenExpiry());
        verify(notificationService).sendEmailNotification(
                42L,
                "Verify your LearnAI account",
                "Click the button below to verify your account. This link expires in 24 hours.",
                "http://localhost:4200/verify-email?token=" + savedStudent.getVerificationToken());
    }

    @Test
    void register_rejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("ada@example.com");

        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.register(request));

        assertEquals("Email already in use", ex.getMessage());
        verify(userRepository, never()).save(any(Student.class));
    }

    @Test
    void login_rejectsUnverifiedAccountBeforeAuthentication() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ada@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setEmail("ada@example.com");
        user.setIsVerified(false);
        user.setIsActive(true);

        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));

        assertEquals("Account not verified. Please check your email.", ex.getMessage());
        verify(authenticationManager, never()).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void verifyEmail_marksUserVerifiedAndClearsToken() {
        User user = new User();
        user.setIsVerified(false);
        user.setVerificationToken("token-123");
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(1));

        when(userRepository.findByVerificationToken("token-123")).thenReturn(Optional.of(user));

        authService.verifyEmail("token-123");

        assertTrue(user.getIsVerified());
        assertNull(user.getVerificationToken());
        assertNull(user.getVerificationTokenExpiry());
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmail_clearsExpiredTokenAndThrows() {
        User user = new User();
        user.setVerificationToken("expired-token");
        user.setVerificationTokenExpiry(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByVerificationToken("expired-token")).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.verifyEmail("expired-token"));

        assertEquals("Verification link has expired. Please request a new verification email.", ex.getMessage());
        assertNull(user.getVerificationToken());
        assertNull(user.getVerificationTokenExpiry());
        verify(userRepository).save(user);
    }

    @Test
    void login_savesLastLoginAndReturnsTokenForVerifiedActiveUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ada@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(7L);
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setEmail("ada@example.com");
        user.setRole(UserRole.STUDENT);
        user.setIsVerified(true);
        user.setIsActive(true);

        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(UserDetailsImpl.class))).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals(7L, response.getId());
        assertNotNull(user.getLastLogin());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_rejectsExpiredToken() {
        User user = new User();
        user.setResetToken("reset-token");
        user.setResetTokenExpiry(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByResetToken("reset-token")).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.resetPassword("reset-token", "new-password"));

        assertEquals("Reset link has expired.", ex.getMessage());
        verify(userRepository, never()).save(user);
    }

    @Test
    void resetPassword_savesEncodedPasswordAndClearsToken() {
        User user = new User();
        user.setResetToken("reset-token");
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByResetToken("reset-token")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        authService.resetPassword("reset-token", "new-password");

        assertEquals("new-hash", user.getPassword());
        assertNull(user.getResetToken());
        assertNull(user.getResetTokenExpiry());
        verify(userRepository).save(user);
    }
}
