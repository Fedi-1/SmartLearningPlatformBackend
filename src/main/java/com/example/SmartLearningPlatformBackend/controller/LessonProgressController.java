package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.lesson.LessonProgressResponse;
import com.example.SmartLearningPlatformBackend.dto.lesson.QuizPassedRequest;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.LessonProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    /**
     * POST /api/lessons/{lessonId}/access
     * Called when a student opens a lesson. Records last_accessed_at.
     */
    @PostMapping("/{lessonId}/access")
    public ResponseEntity<Void> trackAccess(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        lessonProgressService.trackLessonAccess(lessonId, principal.getUser().getId());
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/lessons/{lessonId}/generate-recap
     * Generates (or returns cached) a recap video for the lesson.
     */
    @PostMapping("/{lessonId}/generate-recap")
    public ResponseEntity<Map<String, String>> generateRecap(
            @PathVariable Long lessonId,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        Map<String, String> result = lessonProgressService.generateLessonRecap(lessonId);
        return ResponseEntity.ok(result);
    }
}
