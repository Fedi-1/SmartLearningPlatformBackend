package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.exam.LogSuspiciousActivityRequest;
import com.example.SmartLearningPlatformBackend.dto.exam.SuspiciousActivityDTO;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.SuspiciousActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SuspiciousActivityController {

        private final SuspiciousActivityService suspiciousActivityService;

        /**
         * POST /api/exam-attempts/{attemptId}/suspicious-activity
         * Student calls this during an active exam to report a suspicious event.
         */
        @PostMapping("/api/exam-attempts/{attemptId}/suspicious-activity")
        public ResponseEntity<SuspiciousActivityDTO> logActivity(
                        @PathVariable Long attemptId,
                        @RequestBody LogSuspiciousActivityRequest request,
                        @AuthenticationPrincipal UserDetailsImpl principal) {

                return ResponseEntity.ok(
                                suspiciousActivityService.logActivity(
                                                attemptId, principal.getUser().getId(), request));
        }

        /**
         * GET /api/exam-attempts/{attemptId}/suspicious-activity
         * Student can view their own attempt's activity log.
         */
        @GetMapping("/api/exam-attempts/{attemptId}/suspicious-activity")
        public ResponseEntity<List<SuspiciousActivityDTO>> getForAttempt(
                        @PathVariable Long attemptId,
                        @AuthenticationPrincipal UserDetailsImpl principal) {

                return ResponseEntity.ok(
                                suspiciousActivityService.getForAttempt(
                                                attemptId, principal.getUser().getId()));
        }

        /**
         * GET /api/admin/exam-attempts/{attemptId}/suspicious-activity
         * Admin-only: view any attempt's suspicious activity log.
         */
        @GetMapping("/api/admin/exam-attempts/{attemptId}/suspicious-activity")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<List<SuspiciousActivityDTO>> getForAttemptAdmin(
                        @PathVariable Long attemptId) {

                return ResponseEntity.ok(
                                suspiciousActivityService.getForAttemptAdmin(attemptId));
        }

        /**
         * GET /api/admin/flagged-attempts
         * Admin-only: list all exam attempt IDs that have at least one suspicious
         * activity.
         */
        @GetMapping("/api/admin/flagged-attempts")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<List<Long>> getFlaggedAttemptIds() {
                return ResponseEntity.ok(suspiciousActivityService.getFlaggedAttemptIds());
        }
}
