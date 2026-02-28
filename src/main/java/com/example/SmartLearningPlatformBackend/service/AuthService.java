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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;

        public AuthResponse register(RegisterRequest request) {
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new IllegalArgumentException("Email already in use");
                }

                var student = Student.builder()
                                .firstName(request.getFirstName())
                                .lastName(request.getLastName())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .role(UserRole.STUDENT)
                                .isActive(true)
                                .dateOfBirth(request.getDateOfBirth())
                                .phoneNumber(request.getPhoneNumber())
                                .build();

                var saved = userRepository.save(student);
                var userDetails = new UserDetailsImpl(saved);
                var token = jwtService.generateToken(userDetails);

                return AuthResponse.builder()
                                .token(token)
                                .id(saved.getId())
                                .firstName(saved.getFirstName())
                                .lastName(saved.getLastName())
                                .email(saved.getEmail())
                                .role(saved.getRole())
                                .build();
        }

        public AuthResponse login(LoginRequest request) {
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

                var user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

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
}
