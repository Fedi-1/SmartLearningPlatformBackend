package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.flashcard.FlashcardDueResponse;
import com.example.SmartLearningPlatformBackend.dto.flashcard.FlashcardRateRequest;
import com.example.SmartLearningPlatformBackend.dto.flashcard.FlashcardReviewResponse;
import com.example.SmartLearningPlatformBackend.dto.flashcard.FlashcardSessionResponse;
import com.example.SmartLearningPlatformBackend.enums.DifficultyLevel;
import com.example.SmartLearningPlatformBackend.enums.FlashcardRating;
import com.example.SmartLearningPlatformBackend.models.Flashcard;
import com.example.SmartLearningPlatformBackend.models.FlashcardReview;
import com.example.SmartLearningPlatformBackend.models.Lesson;
import com.example.SmartLearningPlatformBackend.repository.FlashcardRepository;
import com.example.SmartLearningPlatformBackend.repository.FlashcardReviewRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final FlashcardReviewRepository flashcardReviewRepository;
    private final LessonRepository lessonRepository;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private int qualityFor(FlashcardRating rating) {
        return switch (rating) {
            case AGAIN -> 0;
            case GOOD -> 3;
            case EASY -> 5;
        };
    }

    private DifficultyLevel difficultyFrom(float ef) {
        if (ef >= 2.5f)
            return DifficultyLevel.EASY;
        if (ef >= 1.8f)
            return DifficultyLevel.MEDIUM;
        return DifficultyLevel.HARD;
    }

    /** All flashcard IDs that belong to lessons in the given course. */
    private List<Long> courseFlashcardIds(Long courseId, Long studentId) {
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByLessonNumberAsc(courseId);
        List<Long> lessonIds = lessons.stream().map(Lesson::getId).collect(Collectors.toList());
        return lessonIds.stream()
                .flatMap(lid -> flashcardRepository.findByLessonId(lid).stream())
                .map(Flashcard::getId)
                .collect(Collectors.toList());
    }

    private FlashcardDueResponse toDto(Flashcard fc, FlashcardReview r) {
        return FlashcardDueResponse.builder()
                .id(fc.getId())
                .term(fc.getTerm())
                .definition(fc.getDefinition())
                .difficulty(fc.getDifficulty())
                .nextReviewDate(r.getNextReviewDate())
                .easeFactor(r.getEaseFactor())
                .interval(r.getInterval())
                .repetitionCount(r.getRepetitionCount())
                .build();
    }

    // ─── GET /api/courses/{courseId}/flashcards/session ───────────────────────

    @Transactional(readOnly = true)
    public FlashcardSessionResponse getSession(Long courseId, Long studentId) {
        List<Long> fcIds = courseFlashcardIds(courseId, studentId);
        if (fcIds.isEmpty()) {
            return FlashcardSessionResponse.builder()
                    .due(0).flashcards(List.of()).nextUpcomingReviewDate(null).build();
        }

        Map<Long, Flashcard> fcMap = flashcardRepository.findAllById(fcIds)
                .stream().collect(Collectors.toMap(Flashcard::getId, f -> f));

        LocalDate today = LocalDate.now();

        // Due cards ordered by next_review_date ASC (most overdue first)
        List<FlashcardReview> dueReviews = flashcardReviewRepository
                .findByStudentIdAndFlashcardIdInAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(
                        studentId, fcIds, today);

        List<FlashcardDueResponse> dueCards = dueReviews.stream()
                .map(r -> toDto(fcMap.get(r.getFlashcardId()), r))
                .collect(Collectors.toList());

        // Soonest upcoming (future) date — included so the UI can tell the student when
        // to come back
        String nextUpcoming = null;
        if (dueCards.isEmpty()) {
            nextUpcoming = flashcardReviewRepository
                    .findByStudentIdAndFlashcardIdInAndNextReviewDateAfterOrderByNextReviewDateAsc(
                            studentId, fcIds, today)
                    .stream().findFirst()
                    .map(r -> r.getNextReviewDate().toString())
                    .orElse(null);
        }

        return FlashcardSessionResponse.builder()
                .due(dueCards.size())
                .flashcards(dueCards)
                .nextUpcomingReviewDate(nextUpcoming)
                .build();
    }

    // ─── POST /api/flashcards/{flashcardId}/review ────────────────────────────

    @Transactional
    public FlashcardReviewResponse reviewFlashcard(Long flashcardId, Long studentId,
            FlashcardRateRequest request) {

        Flashcard flashcard = flashcardRepository.findById(flashcardId)
                .orElseThrow(() -> new IllegalArgumentException("Flashcard not found."));

        Lesson lesson = lessonRepository.findById(flashcard.getLessonId())
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found."));

        FlashcardReview review = flashcardReviewRepository
                .findByStudentIdAndFlashcardId(studentId, flashcardId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No review record found for this flashcard and student."));

        FlashcardRating rating = FlashcardRating.valueOf(request.getRating().toUpperCase());
        int q = qualityFor(rating);

        // ── SM-2 algorithm (DO NOT MODIFY) ───────────────────────────────────
        float ef = review.getEaseFactor();
        int rep = review.getRepetitionCount();
        int cons = review.getConsecutiveCorrectReviews();

        float newEf = ef + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f));
        newEf = Math.max(1.3f, newEf);

        int newInterval;
        int newRep;
        int newCons;

        if (rating == FlashcardRating.AGAIN) {
            newRep = 0;
            newCons = 0;
            newInterval = 1;
        } else {
            newRep = rep + 1;
            newCons = cons + 1;
            if (newRep == 1) {
                newInterval = 1;
            } else if (newRep == 2) {
                newInterval = 6;
            } else {
                newInterval = (int) Math.ceil(review.getInterval() * newEf);
            }
        }

        LocalDate nextReview = LocalDate.now().plusDays(newInterval);

        review.setEaseFactor(newEf);
        review.setInterval(newInterval);
        review.setRepetitionCount(newRep);
        review.setConsecutiveCorrectReviews(newCons);
        review.setNextReviewDate(nextReview);
        review.setLastReviewedAt(LocalDateTime.now());
        review.setLastRating(rating);
        review.setQualityScore(q);

        flashcard.setDifficulty(difficultyFrom(newEf));
        flashcardRepository.save(flashcard);
        review = flashcardReviewRepository.save(review);
        // ── end SM-2 ─────────────────────────────────────────────────────────

        // Compute nextCard and remainingDue for the session queue
        Long courseId = lesson.getCourseId();
        List<Long> allFcIds = courseFlashcardIds(courseId, studentId);
        List<Long> remainingIds = allFcIds.stream()
                .filter(id -> !id.equals(flashcardId))
                .collect(Collectors.toList());

        LocalDate today = LocalDate.now();
        List<FlashcardReview> stillDue = remainingIds.isEmpty() ? List.of()
                : flashcardReviewRepository
                        .findByStudentIdAndFlashcardIdInAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(
                                studentId, remainingIds, today);

        FlashcardDueResponse nextCard = null;
        if (!stillDue.isEmpty()) {
            FlashcardReview nextR = stillDue.get(0);
            Flashcard nextFc = flashcardRepository.findById(nextR.getFlashcardId()).orElse(null);
            if (nextFc != null) {
                nextCard = toDto(nextFc, nextR);
            }
        }

        return FlashcardReviewResponse.builder()
                .id(review.getId())
                .flashcardId(flashcardId)
                .term(flashcard.getTerm())
                .definition(flashcard.getDefinition())
                .difficulty(flashcard.getDifficulty())
                .easeFactor(review.getEaseFactor())
                .interval(review.getInterval())
                .repetitionCount(review.getRepetitionCount())
                .consecutiveCorrectReviews(review.getConsecutiveCorrectReviews())
                .nextReviewDate(review.getNextReviewDate())
                .lastReviewedAt(review.getLastReviewedAt())
                .lastRating(rating.name())
                .qualityScore(review.getQualityScore())
                .nextCard(nextCard)
                .remainingDue(stillDue.size())
                .build();
    }

    // ─── GET /api/courses/{courseId}/flashcards/due (kept for compatibility) ──

    @Transactional(readOnly = true)
    public List<FlashcardDueResponse> getDueFlashcards(Long courseId, Long studentId) {
        return getSession(courseId, studentId).getFlashcards();
    }
}
