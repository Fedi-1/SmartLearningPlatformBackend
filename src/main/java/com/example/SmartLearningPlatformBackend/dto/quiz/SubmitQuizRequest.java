package com.example.SmartLearningPlatformBackend.dto.quiz;

import lombok.Data;

import java.util.List;

@Data
public class SubmitQuizRequest {

    private List<AnswerRequest> answers;

    /** SUBMITTED | TIME_EXPIRED — defaults to SUBMITTED if absent */
    private String finishReason;
}
