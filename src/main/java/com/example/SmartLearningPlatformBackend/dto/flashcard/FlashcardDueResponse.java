package com.example.SmartLearningPlatformBackend.dto.flashcard;

import com.example.SmartLearningPlatformBackend.enums.DifficultyLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class FlashcardDueResponse {
    private Long id;
    private String term;
    private String definition;
    private DifficultyLevel difficulty;
    private LocalDate nextReviewDate;
    private Float easeFactor;
    private Integer interval;
    private Integer repetitionCount;
}
