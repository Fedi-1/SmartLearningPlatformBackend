package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.FlashcardReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlashcardReviewRepository extends JpaRepository<FlashcardReview, Long> {

    Optional<FlashcardReview> findByStudentIdAndFlashcardId(Long studentId, Long flashcardId);

    List<FlashcardReview> findByStudentIdAndNextReviewDateLessThanEqual(Long studentId, LocalDate date);

    List<FlashcardReview> findByStudentIdAndFlashcardIdIn(Long studentId, List<Long> flashcardIds);

    /**
     * All due reviews for a student restricted to a set of flashcard IDs,
     * ordered by next_review_date ascending (most overdue first).
     */
    List<FlashcardReview> findByStudentIdAndFlashcardIdInAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(
            Long studentId, List<Long> flashcardIds, LocalDate date);

    /**
     * The soonest upcoming (future) review for a student, restricted to a flashcard
     * set.
     */
    List<FlashcardReview> findByStudentIdAndFlashcardIdInAndNextReviewDateAfterOrderByNextReviewDateAsc(
            Long studentId, List<Long> flashcardIds, LocalDate date);
}
