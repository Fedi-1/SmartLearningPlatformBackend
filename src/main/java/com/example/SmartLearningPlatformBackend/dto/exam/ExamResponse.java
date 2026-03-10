package com.example.SmartLearningPlatformBackend.dto.exam;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExamResponse {
    private Long id;
    private String title;
    private Integer passingScore;
    private Integer maxAttempts;
    private Integer totalPoints;
    private Integer timeLimitMinutes;
}
