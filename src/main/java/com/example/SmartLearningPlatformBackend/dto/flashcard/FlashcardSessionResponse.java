package com.example.SmartLearningPlatformBackend.dto.flashcard;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FlashcardSessionResponse {
    private int due;
    private List<FlashcardDueResponse> flashcards;
    /** ISO date string of the soonest upcoming review (null if no reviews exist) */
    private String nextUpcomingReviewDate;
}
