package com.example.SmartLearningPlatformBackend.dto.exam;

import lombok.Data;

@Data
public class ExamAnswerRequest {
    private Long questionId;
    private String studentAnswer;
}
