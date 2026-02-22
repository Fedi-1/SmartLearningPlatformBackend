package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.course.CourseDetailResponse;
import com.example.SmartLearningPlatformBackend.dto.course.LessonProgressItem;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDetailResponse> getCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        CourseDetailResponse response = courseService.getCourseById(courseId, principal.getUser().getId());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/courses/{courseId}/my-progress
     * Returns the LessonProgress records (locked/completed/quizPassed) for
     * every lesson in this course for the authenticated student.
     */
    @GetMapping("/{courseId}/my-progress")
    public ResponseEntity<List<LessonProgressItem>> getCourseProgress(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        List<LessonProgressItem> progress = courseService.getCourseProgress(courseId, principal.getUser().getId());
        return ResponseEntity.ok(progress);
    }
}
