package com.example.SmartLearningPlatformBackend.dto.course;

import lombok.Builder;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Data
@Builder
public class LessonResponse {
    private Long id;
    private Integer lessonNumber;
    private String title;
    private String summary;
    private String content;
    @JsonProperty("isLocked")
    private boolean isLocked;
    private Long quizId;
    private List<QuizQuestionResponse> quiz;
    private List<FlashcardResponse> flashcards;
}
