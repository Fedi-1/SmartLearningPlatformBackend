package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.lesson.LessonProgressResponse;
import com.example.SmartLearningPlatformBackend.models.Lesson;
import com.example.SmartLearningPlatformBackend.models.LessonProgress;
import com.example.SmartLearningPlatformBackend.models.Quiz;
import com.example.SmartLearningPlatformBackend.models.QuizAttempt;
import com.example.SmartLearningPlatformBackend.repository.LessonProgressRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizAttemptRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonProgressService {

    private final LessonProgressRepository lessonProgressRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRepository quizRepository;
    private final LessonRepository lessonRepository;

    /**
     * Called after a quiz attempt is submitted.
     * - If attempt is passed → mark lesson complete + quizPassed, then unlock next
     * lesson.
     * - If attempts exhausted → unlock next lesson regardless (student can still
     * move on).
     */
    @Transactional
    public LessonProgressResponse processQuizAttempt(Long quizAttemptId, Long studentId) {

        QuizAttempt attempt = quizAttemptRepository.findById(quizAttemptId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz attempt not found."));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("Access denied.");
        }

        Quiz quiz = quizRepository.findById(attempt.getQuizId())
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found."));

        Lesson currentLesson = lessonRepository.findById(quiz.getLessonId())
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found."));

        LessonProgress currentProgress = lessonProgressRepository
                .findByStudentIdAndLessonId(studentId, currentLesson.getId())
                .orElseThrow(() -> new IllegalArgumentException("Lesson progress not found."));

        boolean passed = Boolean.TRUE.equals(attempt.getIsPassed());
        int attemptsUsed = quizAttemptRepository.countByStudentIdAndQuizId(studentId, quiz.getId());
        boolean attemptsExhausted = attemptsUsed >= quiz.getMaxAttempts();

        // Update current lesson progress if passed
        if (passed) {
            currentProgress.setIsCompleted(true);
            currentProgress.setQuizPassed(true);
            currentProgress.setCompletedAt(LocalDateTime.now());
            lessonProgressRepository.save(currentProgress);
        }

        // Unlock next lesson if passed OR if all attempts are exhausted
        if (passed || attemptsExhausted) {
            unlockNextLesson(studentId, currentLesson);
        }

        return toResponse(currentProgress);
    }

    /**
     * Returns the LessonProgress for a given lesson and student.
     */
    @Transactional(readOnly = true)
    public LessonProgressResponse getLessonProgress(Long lessonId, Long studentId) {

        LessonProgress progress = lessonProgressRepository
                .findByStudentIdAndLessonId(studentId, lessonId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson progress not found."));

        return toResponse(progress);
    }

    // -------------------------------------------------------------------------

    private void unlockNextLesson(Long studentId, Lesson currentLesson) {

        List<Lesson> courseLessons = lessonRepository
                .findByCourseIdOrderByLessonNumberAsc(currentLesson.getCourseId());

        courseLessons.stream()
                .filter(l -> l.getLessonNumber() == currentLesson.getLessonNumber() + 1)
                .findFirst()
                .ifPresent(nextLesson -> lessonProgressRepository
                        .findByStudentIdAndLessonId(studentId, nextLesson.getId())
                        .ifPresent(nextProgress -> {
                            nextProgress.setIsLocked(false);
                            lessonProgressRepository.save(nextProgress);
                        }));
    }

    private LessonProgressResponse toResponse(LessonProgress p) {
        return LessonProgressResponse.builder()
                .id(p.getId())
                .lessonId(p.getLessonId())
                .isCompleted(Boolean.TRUE.equals(p.getIsCompleted()))
                .isLocked(Boolean.TRUE.equals(p.getIsLocked()))
                .quizPassed(Boolean.TRUE.equals(p.getQuizPassed()))
                .build();
    }
}
