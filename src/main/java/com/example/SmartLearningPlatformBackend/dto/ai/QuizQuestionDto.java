package com.example.SmartLearningPlatformBackend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class QuizQuestionDto {

    @JsonProperty("questionType")
    private String questionType;

    @JsonProperty("question")
    private String question;

    @JsonProperty("options")
    private List<String> options;

    @JsonProperty("correctAnswer")
    private String correctAnswer;

    @JsonProperty("explanation")
    private String explanation;

    @JsonProperty("difficulty")
    private String difficulty;
}
