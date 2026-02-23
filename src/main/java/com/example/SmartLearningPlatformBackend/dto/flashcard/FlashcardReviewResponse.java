package com.example.SmartLearningPlatformBackend.dto.flashcard;

import com.example.SmartLearningPlatformBackend.enums.DifficultyLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class FlashcardReviewResponse {
    private Long id;
    private Long flashcardId;
    private String term;
    private String definition;
    private DifficultyLevel difficulty;
    private Float easeFactor;
    private Integer interval;
    private Integer repetitionCount;
    private Integer consecutiveCorrectReviews;
    private LocalDate nextReviewDate;
    private LocalDateTime lastReviewedAt;
    private String lastRating;
    private Integer qualityScore;
    /**
     * Next due flashcard in the same course, or null if the session is complete.
     */
    private FlashcardDueResponse nextCard;
    /** How many cards remain due in this course after this rating. */
    private Integer remainingDue;
}
