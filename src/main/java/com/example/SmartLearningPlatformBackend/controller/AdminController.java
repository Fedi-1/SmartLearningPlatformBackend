// src/main/java/.../controller/AdminController.java
package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.admin.*;
import com.example.SmartLearningPlatformBackend.dto.student.ChangePasswordRequest;
import com.example.SmartLearningPlatformBackend.dto.student.StudentProfileResponse;
import com.example.SmartLearningPlatformBackend.dto.student.UpdateProfileRequest;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/activity-chart")
    public ResponseEntity<List<ActivityChartPoint>> getActivityChart(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(adminService.getActivityChart(days));
    }

    @GetMapping("/category-distribution")
    public ResponseEntity<List<CategoryDistributionPoint>> getCategoryDistribution() {
        return ResponseEntity.ok(adminService.getCategoryDistribution());
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<List<RecentActivityEntry>> getRecentActivity(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(adminService.getRecentActivity(limit));
    }

    @GetMapping("/students")
    public ResponseEntity<List<StudentSummaryResponse>> getAllStudents() {
        return ResponseEntity.ok(adminService.getAllStudents());
    }

    @GetMapping("/students/{studentId}/detail")
    public ResponseEntity<StudentDetailResponse> getStudentDetail(@PathVariable Long studentId) {
        return ResponseEntity.ok(adminService.getStudentDetail(studentId));
    }

    @GetMapping("/students/{studentId}/exam-attempts")
    public ResponseEntity<List<StudentExamAttemptItem>> getStudentExamAttempts(@PathVariable Long studentId) {
        return ResponseEntity.ok(adminService.getStudentExamAttempts(studentId));
    }

    @PatchMapping("/students/{studentId}/toggle-status")
    public ResponseEntity<Boolean> toggleStudentStatus(@PathVariable Long studentId) {
        return ResponseEntity.ok(adminService.toggleStudentStatus(studentId));
    }

    @PatchMapping("/certificates/{id}/approve")
    public ResponseEntity<Void> approveCertificate(@PathVariable Long id) {
        adminService.approveCertificate(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/certificates/{id}/revoke")
    public ResponseEntity<Void> revokeCertificate(@PathVariable Long id) {
        adminService.revokeCertificate(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/certificates")
    public ResponseEntity<List<AdminCertificateItem>> getAllCertificates() {
        return ResponseEntity.ok(adminService.getAllCertificates());
    }

    @GetMapping("/exam-attempts")
    public ResponseEntity<List<AdminExamAttemptItem>> getAllExamAttempts() {
        return ResponseEntity.ok(adminService.getAllExamAttempts());
    }

    @GetMapping("/activity-logs")
    public ResponseEntity<ActivityLogPageResponse> getActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long studentId) {
        return ResponseEntity.ok(adminService.getActivityLogs(page, size, action, studentId));
    }

    @GetMapping("/profile")
    public ResponseEntity<StudentProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(adminService.getAdminProfile(principal.getUser().getId()));
    }

    @PutMapping("/profile")
    public ResponseEntity<StudentProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(adminService.updateAdminProfile(principal.getUser().getId(), request));
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestBody ChangePasswordRequest request) {
        try {
            adminService.changeAdminPassword(principal.getUser().getId(), request);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        }
    }
}
