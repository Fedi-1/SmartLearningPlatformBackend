package com.example.SmartLearningPlatformBackend.dto.auth;

import com.example.SmartLearningPlatformBackend.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String token;
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private UserRole role;
}