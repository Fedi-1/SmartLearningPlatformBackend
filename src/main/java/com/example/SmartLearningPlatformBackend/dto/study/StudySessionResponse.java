package com.example.SmartLearningPlatformBackend.dto.study;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudySessionResponse {
    private Long sessionId;
    private boolean active;
    private long accumulatedSeconds;
}
