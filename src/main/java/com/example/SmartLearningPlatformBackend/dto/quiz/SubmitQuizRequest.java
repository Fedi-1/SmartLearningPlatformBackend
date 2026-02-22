package com.example.SmartLearningPlatformBackend.dto.quiz;

import lombok.Data;

import java.util.List;

@Data
public class SubmitQuizRequest {

    private List<AnswerRequest> answers;
}
