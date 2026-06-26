package com.example.SmartLearningPlatformBackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FlashcardServiceTest {

    @Mock
    private FlashcardRepository flashcardRepository;
    @Mock
    private FlashcardReviewRepository flashcardReviewRepository;
    @Mock
    private LessonRepository lessonRepository;

    @InjectMocks
    private FlashcardService flashcardService;

    @Test
    void getSession_returnsEmptySessionWhenCourseHasNoFlashcards() {
        when(lessonRepository.findByCourseIdOrderByLessonNumberAsc(1L)).thenReturn(List.of());

        FlashcardSessionResponse response = flashcardService.getSession(1L, 7L);

        assertEquals(0, response.getDue());
        assertTrue(response.getFlashcards().isEmpty());
        assertNull(response.getNextUpcomingReviewDate());
    }

    @Test
    void getSession_returnsDueCardsForCourse() {
        Lesson lesson = Lesson.builder().id(2L).courseId(1L).build();
        Flashcard flashcard = Flashcard.builder()
                .id(3L)
                .lessonId(2L)
                .term("API")
                .definition("Application Programming Interface")
                .difficulty(DifficultyLevel.MEDIUM)
                .build();
        FlashcardReview review = FlashcardReview.builder()
                .id(4L)
                .studentId(7L)
                .flashcardId(3L)
                .nextReviewDate(LocalDate.now())
                .easeFactor(2.5f)
                .interval(1)
                .repetitionCount(0)
                .build();

        when(lessonRepository.findByCourseIdOrderByLessonNumberAsc(1L)).thenReturn(List.of(lesson));
        when(flashcardRepository.findByLessonId(2L)).thenReturn(List.of(flashcard));
        when(flashcardRepository.findAllById(List.of(3L))).thenReturn(List.of(flashcard));
        when(flashcardReviewRepository
                .findByStudentIdAndFlashcardIdInAndNextReviewDateLessThanEqualOrderByNextReviewDateAsc(
                        any(), any(), any()))
                .thenReturn(List.of(review));

        FlashcardSessionResponse response = flashcardService.getSession(1L, 7L);

        assertEquals(1, response.getDue());
        assertEquals("API", response.getFlashcards().get(0).getTerm());
    }

    @Test
    void reviewFlashcard_againResetsRepetitionAndSchedulesTomorrow() {
        Flashcard flashcard = Flashcard.builder()
                .id(3L)
                .lessonId(2L)
                .term("API")
                .definition("Application Programming Interface")
                .difficulty(DifficultyLevel.EASY)
                .build();
        Lesson lesson = Lesson.builder().id(2L).courseId(1L).build();
        FlashcardReview review = FlashcardReview.builder()
                .id(4L)
                .studentId(7L)
                .flashcardId(3L)
                .easeFactor(2.5f)
                .interval(6)
                .repetitionCount(2)
                .consecutiveCorrectReviews(2)
                .nextReviewDate(LocalDate.now())
                .build();
        FlashcardRateRequest request = new FlashcardRateRequest();
        request.setRating("AGAIN");

        when(flashcardRepository.findById(3L)).thenReturn(Optional.of(flashcard));
        when(lessonRepository.findById(2L)).thenReturn(Optional.of(lesson));
        when(flashcardReviewRepository.findByStudentIdAndFlashcardId(7L, 3L)).thenReturn(Optional.of(review));
        when(flashcardRepository.save(flashcard)).thenReturn(flashcard);
        when(flashcardReviewRepository.save(review)).thenReturn(review);
        when(lessonRepository.findByCourseIdOrderByLessonNumberAsc(1L)).thenReturn(List.of(lesson));
        when(flashcardRepository.findByLessonId(2L)).thenReturn(List.of(flashcard));

        FlashcardReviewResponse response = flashcardService.reviewFlashcard(3L, 7L, request);

        assertEquals(0, response.getRepetitionCount());
        assertEquals(1, response.getInterval());
        assertEquals("AGAIN", response.getLastRating());
        assertEquals(LocalDate.now().plusDays(1), response.getNextReviewDate());
        verify(flashcardReviewRepository).save(review);
    }
}
