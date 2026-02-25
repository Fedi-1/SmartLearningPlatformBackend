package com.example.SmartLearningPlatformBackend.dto.exam;

import lombok.Data;

import java.util.List;

@Data
public class SubmitExamRequest {
    private List<ExamAnswerRequest> answers;
}
