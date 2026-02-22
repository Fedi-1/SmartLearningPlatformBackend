package com.example.SmartLearningPlatformBackend.dto.course;

import com.example.SmartLearningPlatformBackend.enums.DifficultyLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuizQuestionResponse {
    private Long id;
    private Integer questionNumber;
    private String questionText;
    private String questionType; // "MCQ" | "TRUE_FALSE" | "FILL_BLANK"
    private List<String> options;
    private String correctAnswer;
    private String explanation;
    private DifficultyLevel difficulty;
}
