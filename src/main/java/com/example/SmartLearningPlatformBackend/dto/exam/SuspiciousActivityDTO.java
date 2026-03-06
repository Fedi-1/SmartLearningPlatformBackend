package com.example.SmartLearningPlatformBackend.dto.exam;

import com.example.SmartLearningPlatformBackend.enums.SuspiciousActivityType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SuspiciousActivityDTO {
    private Long id;
    private Long examAttemptId;
    private SuspiciousActivityType activityType;
    private Integer count;
    private LocalDateTime detectedAt;
    /** Running total of all suspicious events for this attempt. */
    private Integer totalCount;
}
