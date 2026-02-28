package com.example.SmartLearningPlatformBackend.dto.exam;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ExamAttemptResponse {
    private Long id;
    private Long examId;
    private Integer attemptNumber;
    private Integer score;
    private Boolean isPassed;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private String finishReason;
    private Integer attemptsUsed;
    private Integer maxAttempts;
    private Integer timeLimitMinutes;
    private Boolean hasCertificate;
    private List<ExamQuestionResponse> questions;
}
