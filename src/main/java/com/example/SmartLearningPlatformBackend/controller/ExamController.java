package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.exam.*;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.ExamGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExamController {

        private final ExamGenerationService examGenerationService;

        /** POST /api/courses/{courseId}/generate-exam */
        @PostMapping("/api/courses/{courseId}/generate-exam")
        public ResponseEntity<ExamResponse> generateExam(
                        @PathVariable Long courseId,
                        @AuthenticationPrincipal UserDetailsImpl principal) {

                return ResponseEntity.ok(
                                examGenerationService.generateExamForCourse(courseId, principal.getUser().getId()));
        }

        /** GET /api/courses/{courseId}/exam */
        @GetMapping("/api/courses/{courseId}/exam")
        public ResponseEntity<ExamResponse> getExam(
                        @PathVariable Long courseId,
                        @AuthenticationPrincipal UserDetailsImpl principal) {

                return ResponseEntity.ok(
                                examGenerationService.getExamForCourse(courseId, principal.getUser().getId()));
        }

        /** POST /api/exams/{examId}/start */
        @PostMapping("/api/exams/{examId}/start")
        public ResponseEntity<ExamAttemptResponse> startAttempt(
                        @PathVariable Long examId,
                        @AuthenticationPrincipal UserDetailsImpl principal) {

                return ResponseEntity.ok(
                                examGenerationService.startAttempt(examId, principal.getUser().getId()));
        }

        /** POST /api/exam-attempts/{attemptId}/submit */
        @PostMapping("/api/exam-attempts/{attemptId}/submit")
        public ResponseEntity<SubmitExamResponse> submitAttempt(
                        @PathVariable Long attemptId,
                        @RequestBody SubmitExamRequest request,
                        @AuthenticationPrincipal UserDetailsImpl principal) {

                return ResponseEntity.ok(
                                examGenerationService.submitAttempt(attemptId, principal.getUser().getId(), request));
        }

        /** POST /api/exam-attempts/{attemptId}/abandon */
        @PostMapping("/api/exam-attempts/{attemptId}/abandon")
        public ResponseEntity<ExamAttemptResponse> abandonAttempt(
                        @PathVariable Long attemptId,
                        @AuthenticationPrincipal UserDetailsImpl principal) {

                return ResponseEntity.ok(
                                examGenerationService.abandonAttempt(attemptId, principal.getUser().getId()));
        }

        /** GET /api/exams/{examId}/my-attempts */
        @GetMapping("/api/exams/{examId}/my-attempts")
        public ResponseEntity<List<ExamAttemptResponse>> getMyAttempts(
                        @PathVariable Long examId,
                        @AuthenticationPrincipal UserDetailsImpl principal) {

                return ResponseEntity.ok(
                                examGenerationService.getMyAttempts(examId, principal.getUser().getId()));
        }
}
