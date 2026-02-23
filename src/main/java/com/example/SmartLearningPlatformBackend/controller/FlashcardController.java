package com.example.SmartLearningPlatformBackend.controller;

import com.example.SmartLearningPlatformBackend.dto.flashcard.FlashcardDueResponse;
import com.example.SmartLearningPlatformBackend.dto.flashcard.FlashcardRateRequest;
import com.example.SmartLearningPlatformBackend.dto.flashcard.FlashcardReviewResponse;
import com.example.SmartLearningPlatformBackend.dto.flashcard.FlashcardSessionResponse;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.FlashcardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FlashcardController {

    private final FlashcardService flashcardService;

    /**
     * POST /api/flashcards/{flashcardId}/review
     * Rate a flashcard using SM-2: AGAIN, GOOD, or EASY.
     * Response includes nextCard (next due flashcard in the course) and
     * remainingDue count.
     */
    @PostMapping("/api/flashcards/{flashcardId}/review")
    public ResponseEntity<FlashcardReviewResponse> reviewFlashcard(
            @PathVariable Long flashcardId,
            @RequestBody FlashcardRateRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        return ResponseEntity.ok(
                flashcardService.reviewFlashcard(flashcardId, principal.getUser().getId(), request));
    }

    /**
     * GET /api/courses/{courseId}/flashcards/session
     * Returns today's review session: due count, ordered flashcard list, and next
     * upcoming date.
     */
    @GetMapping("/api/courses/{courseId}/flashcards/session")
    public ResponseEntity<FlashcardSessionResponse> getSession(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        return ResponseEntity.ok(
                flashcardService.getSession(courseId, principal.getUser().getId()));
    }

    /**
     * GET /api/courses/{courseId}/flashcards/due
     * Kept for backwards compatibility — returns the same ordered list as
     * session.flashcards.
     */
    @GetMapping("/api/courses/{courseId}/flashcards/due")
    public ResponseEntity<List<FlashcardDueResponse>> getDueFlashcards(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        return ResponseEntity.ok(
                flashcardService.getDueFlashcards(courseId, principal.getUser().getId()));
    }
}
