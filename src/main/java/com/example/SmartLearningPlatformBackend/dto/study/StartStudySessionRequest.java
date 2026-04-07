package com.example.SmartLearningPlatformBackend.dto.study;

import lombok.Data;

@Data
public class StartStudySessionRequest {
    private Long courseId;
    private Long lessonId;
}
