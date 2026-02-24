package com.example.SmartLearningPlatformBackend.dto.dashboard;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DashboardResponse {

    private StatsDto stats;
    private List<CourseProgressDto> courses;
    private List<ActivityDto> recentActivity;
    private List<FlashcardsDueByCourseDto> flashcardsDue;

    @Data
    @Builder
    public static class StatsDto {
        private int totalCourses;
        private int completedCourses;
        private int totalLessons;
        private int completedLessons;
        private int totalQuizAttempts;
        private int passedQuizAttempts;
        private int averageQuizScore;
        private int flashcardsDueToday;
        private int totalFlashcards;
    }

    @Data
    @Builder
    public static class CourseProgressDto {
        private Long courseId;
        private String title;
        private String category;
        private int progressPercentage;
        private int totalLessons;
        private int completedLessons;
        private int quizzesPassed;
        private int totalQuizzes;
        private boolean examPassed;
        private LocalDateTime lastAccessedAt;
    }

    @Data
    @Builder
    public static class ActivityDto {
        private String action;
        private String description;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    public static class FlashcardsDueByCourseDto {
        private Long courseId;
        private String courseTitle;
        private int dueCount;
    }
}
