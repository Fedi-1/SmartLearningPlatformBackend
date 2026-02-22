package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.lesson.LessonProgressResponse;
import com.example.SmartLearningPlatformBackend.dto.lesson.QuizPassedRequest;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.LessonProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonProgressController {

    private final LessonProgressService lessonProgressService;

    /**
     * POST /api/lessons/quiz-passed
     * Body: { "quizAttemptId": 5 }
     *
     * Processes the quiz attempt result:
     * - Marks lesson as completed + quizPassed if the attempt was passed.
     * - Unlocks the next lesson if passed OR if all attempts are exhausted.
     */
    @PostMapping("/quiz-passed")
    public ResponseEntity<LessonProgressResponse> quizPassed(
            @RequestBody QuizPassedRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        Long studentId = principal.getUser().getId();
        LessonProgressResponse response = lessonProgressService.processQuizAttempt(
                request.getQuizAttemptId(), studentId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/lessons/{lessonId}/progress
     *
     * Returns the LessonProgress record for the authenticated student.
     */
    @GetMapping("/{lessonId}/progress")
    public ResponseEntity<LessonProgressResponse> getLessonProgress(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        Long studentId = principal.getUser().getId();
        LessonProgressResponse response = lessonProgressService.getLessonProgress(lessonId, studentId);
        return ResponseEntity.ok(response);
    }
}
