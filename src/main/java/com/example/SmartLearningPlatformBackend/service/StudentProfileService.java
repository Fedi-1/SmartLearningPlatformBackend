package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.student.ChangePasswordRequest;
import com.example.SmartLearningPlatformBackend.dto.student.StudentProfileResponse;
import com.example.SmartLearningPlatformBackend.dto.student.UpdateProfileRequest;
import com.example.SmartLearningPlatformBackend.models.Student;
import com.example.SmartLearningPlatformBackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentProfileResponse getProfile(Long userId) {
        Student student = findStudent(userId);
        return toResponse(student);
    }

    @Transactional
    public StudentProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        Student student = findStudent(userId);

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            student.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            student.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            student.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getDateOfBirth() != null) {
            student.setDateOfBirth(request.getDateOfBirth());
        }

        Student saved = (Student) userRepository.save(student);
        return toResponse(saved);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        Student student = findStudent(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), student.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        student.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(student);
    }

    private Student findStudent(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u instanceof Student)
                .map(u -> (Student) u)
                .orElseThrow(() -> new IllegalStateException("Student not found"));
    }

    private StudentProfileResponse toResponse(Student s) {
        return StudentProfileResponse.builder()
                .id(s.getId())
                .firstName(s.getFirstName())
                .lastName(s.getLastName())
                .email(s.getEmail())
                .phoneNumber(s.getPhoneNumber())
                .dateOfBirth(s.getDateOfBirth())
                .build();
    }
}
