package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.quiz.*;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.QuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /**
     * POST /api/quizzes/{quizId}/start
     * Start a new quiz attempt. Returns 403 if max attempts reached.
     */
    @PostMapping("/api/quizzes/{quizId}/start")
    public ResponseEntity<QuizAttemptResponse> startAttempt(
            @PathVariable Long quizId,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        return ResponseEntity.ok(
                quizService.startAttempt(quizId, principal.getUser().getId()));
    }

    /**
     * POST /api/quiz-attempts/{attemptId}/submit
     * Submit all answers for an attempt. Scores it, saves answers, triggers unlock.
     */
    @PostMapping("/api/quiz-attempts/{attemptId}/submit")
    public ResponseEntity<SubmitQuizResponse> submitAttempt(
            @PathVariable Long attemptId,
            @RequestBody SubmitQuizRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        return ResponseEntity.ok(
                quizService.submitAttempt(attemptId, principal.getUser().getId(), request));
    }

    /**
     * GET /api/quizzes/{quizId}/my-attempts
     * Returns all past attempts for the authenticated student on this quiz.
     */
    @GetMapping("/api/quizzes/{quizId}/my-attempts")
    public ResponseEntity<List<QuizAttemptResponse>> getMyAttempts(
            @PathVariable Long quizId,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        return ResponseEntity.ok(
                quizService.getMyAttempts(quizId, principal.getUser().getId()));
    }
}
