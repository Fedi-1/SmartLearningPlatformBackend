package com.example.SmartLearningPlatformBackend.dto.exam;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExamResponse {
    private Long id;
    private String title;
    private Integer passingScore;
    private Integer maxAttempts;
    private Integer totalPoints;
    private Integer sectionEasyCount;
    private Integer sectionMediumCount;
    private Integer sectionHardCount;
    private LocalDateTime createdAt;
}
