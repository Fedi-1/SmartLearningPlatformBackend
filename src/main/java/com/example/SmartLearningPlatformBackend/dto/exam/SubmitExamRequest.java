package com.example.SmartLearningPlatformBackend.dto.exam;

import lombok.Data;

import java.util.List;

@Data
public class SubmitExamRequest {
    private List<ExamAnswerRequest> answers;

    /** SUBMITTED | TIME_EXPIRED — defaults to SUBMITTED if absent */
    private String finishReason;
}
