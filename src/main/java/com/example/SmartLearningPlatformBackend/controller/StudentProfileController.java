package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.student.ChangePasswordRequest;
import com.example.SmartLearningPlatformBackend.dto.student.StudentProfileResponse;
import com.example.SmartLearningPlatformBackend.dto.student.UpdateProfileRequest;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.StudentProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentProfileController {

    private final StudentProfileService studentProfileService;

    @GetMapping("/profile")
    public ResponseEntity<StudentProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(
                studentProfileService.getProfile(principal.getUser().getId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<StudentProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(
                studentProfileService.updateProfile(principal.getUser().getId(), request));
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            studentProfileService.changePassword(principal.getUser().getId(), request);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        }
    }
}
