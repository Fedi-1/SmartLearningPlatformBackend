package com.example.SmartLearningPlatformBackend.dto.student;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate dateOfBirth;
}
