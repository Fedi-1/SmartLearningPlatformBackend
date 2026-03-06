package com.example.SmartLearningPlatformBackend.dto.exam;

import com.example.SmartLearningPlatformBackend.enums.SuspiciousActivityType;
import lombok.Data;

@Data
public class LogSuspiciousActivityRequest {
    private SuspiciousActivityType activityType;
    /**
     * Client-measured elapsed seconds since attempt start (used for
     * UNUSUAL_TIMING).
     */
    private Integer clientElapsedSeconds;
}
