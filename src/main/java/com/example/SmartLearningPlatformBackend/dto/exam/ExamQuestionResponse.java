package com.example.SmartLearningPlatformBackend.dto.exam;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExamQuestionResponse {
    private Long id;
    private Integer questionNumber;
    private String questionText;
    private String questionType;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private String explanation;
    private String difficulty;
    private Integer sectionNumber;
    private Integer pointsWorth;
    // correctAnswer is intentionally excluded from this DTO (used during active
    // attempt)
}
