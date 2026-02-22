package com.example.SmartLearningPlatformBackend.dto.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.SmartLearningPlatformBackend.dto.lesson.LessonProgressResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmitQuizResponse {

    private Long attemptId;
    private Integer score;
    @JsonProperty("isPassed")
    private boolean isPassed;
    private Integer attemptsUsed;
    private Integer maxAttempts;
    private boolean attemptsExhausted;
    private LessonProgressResponse lessonProgress;
}
