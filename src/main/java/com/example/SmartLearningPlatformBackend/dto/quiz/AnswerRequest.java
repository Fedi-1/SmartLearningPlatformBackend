package com.example.SmartLearningPlatformBackend.dto.quiz;

import lombok.Data;

@Data
public class AnswerRequest {

    private Long questionId;
    private String studentAnswer;
}
