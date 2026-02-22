package com.example.SmartLearningPlatformBackend.dto.quiz;

import com.example.SmartLearningPlatformBackend.dto.course.QuizQuestionResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuizAttemptResponse {

    private Long id;
    private Long quizId;
    private Integer attemptNumber;
    private Integer score;
    @JsonProperty("isPassed")
    private boolean isPassed;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private String finishReason;
    private Integer attemptsUsed;
    private Integer maxAttempts;

    /**
     * The 5 randomly selected questions for this attempt (only present on start,
     * null on history).
     */
    private List<QuizQuestionResponse> questions;
}
