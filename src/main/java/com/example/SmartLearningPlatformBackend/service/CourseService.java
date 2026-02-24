package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.ai.AiCourseResponse;
import com.example.SmartLearningPlatformBackend.dto.ai.FlashcardDto;
import com.example.SmartLearningPlatformBackend.dto.ai.LessonDto;
import com.example.SmartLearningPlatformBackend.dto.course.*;
import com.example.SmartLearningPlatformBackend.enums.DifficultyLevel;
import com.example.SmartLearningPlatformBackend.models.*;
import com.example.SmartLearningPlatformBackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

        private final CourseRepository courseRepository;
        private final LessonRepository lessonRepository;
        private final QuizRepository quizRepository;
        private final FlashcardRepository flashcardRepository;
        private final FlashcardReviewRepository flashcardReviewRepository;
        private final LessonProgressRepository lessonProgressRepository;

        @Transactional
        public Course generateAndSave(AiCourseResponse aiResponse, Document document, Student student) {

                Course course = Course.builder()
                                .documentId(document.getId())
                                .studentId(student.getId())
                                .title(aiResponse.getCourseTitle())
                                .category(aiResponse.getCategory())
                                .description("Auto-generated course from document: " + document.getFileName())
                                .build();
                course = courseRepository.save(course);

                List<Lesson> savedLessons = new ArrayList<>();

                for (LessonDto lessonDto : aiResponse.getLessons()) {

                        Lesson lesson = Lesson.builder()
                                        .courseId(course.getId())
                                        .lessonNumber(lessonDto.getLessonNumber())
                                        .title(lessonDto.getTitle())
                                        .content(lessonDto.getContent() != null ? lessonDto.getContent()
                                                        : lessonDto.getSummary())
                                        .summary(lessonDto.getSummary())
                                        .estimatedReadTime(lessonDto.getEstimatedReadTime())
                                        .build();
                        lesson = lessonRepository.save(lesson);

                        Quiz quiz = Quiz.builder()
                                        .lessonId(lesson.getId())
                                        .title("Quiz — " + lessonDto.getTitle())
                                        .passingScore(70)
                                        .maxAttempts(3)
                                        .build();
                        quiz = quizRepository.save(quiz);

                        if (lessonDto.getFlashcards() != null) {
                                for (FlashcardDto fcDto : lessonDto.getFlashcards()) {
                                        Flashcard flashcard = Flashcard.builder()
                                                        .lessonId(lesson.getId())
                                                        .term(fcDto.getTerm())
                                                        .definition(fcDto.getDefinition())
                                                        .difficulty(DifficultyLevel.MEDIUM)
                                                        .build();
                                        Flashcard savedFlashcard = flashcardRepository.save(flashcard);
                                        flashcardReviewRepository.save(FlashcardReview.builder()
                                                        .studentId(student.getId())
                                                        .flashcardId(savedFlashcard.getId())
                                                        .easeFactor(2.5f)
                                                        .interval(0)
                                                        .repetitionCount(0)
                                                        .consecutiveCorrectReviews(0)
                                                        .nextReviewDate(LocalDate.now())
                                                        .build());
                                }
                        }

                        savedLessons.add(lesson);
                }

                // Initialise LessonProgress: first lesson unlocked, all others locked
                for (int i = 0; i < savedLessons.size(); i++) {
                        Lesson saved = savedLessons.get(i);
                        LessonProgress progress = LessonProgress.builder()
                                        .studentId(student.getId())
                                        .lessonId(saved.getId())
                                        .isLocked(i != 0)
                                        .isCompleted(false)
                                        .quizPassed(false)
                                        .build();
                        lessonProgressRepository.save(progress);
                }

                return course;
        }

        @Transactional(readOnly = true)
        public CourseDetailResponse getCourseById(Long courseId, Long studentId) {
                Course course = courseRepository.findByIdAndIsDeletedFalse(courseId)
                                .orElseThrow(() -> new IllegalArgumentException("Course not found."));
                if (!course.getStudentId().equals(studentId)) {
                        throw new IllegalArgumentException("Access denied.");
                }

                List<Lesson> lessons = lessonRepository.findByCourseIdOrderByLessonNumberAsc(courseId);

                // Build a map: lessonId → isLocked for this student
                List<Long> lessonIds = lessons.stream().map(Lesson::getId).collect(Collectors.toList());
                java.util.Map<Long, Boolean> lockedMap = lessonProgressRepository
                                .findByStudentIdAndLessonIdIn(studentId, lessonIds)
                                .stream()
                                .collect(Collectors.toMap(
                                                com.example.SmartLearningPlatformBackend.models.LessonProgress::getLessonId,
                                                lp -> Boolean.TRUE.equals(lp.getIsLocked())));

                List<LessonResponse> lessonResponses = lessons.stream().map(lesson -> {
                        Quiz quiz = quizRepository.findByLessonId(lesson.getId()).orElse(null);

                        List<QuizQuestionResponse> questions = List.of();

                        List<FlashcardResponse> flashcards = flashcardRepository
                                        .findByLessonId(lesson.getId())
                                        .stream()
                                        .map(fc -> FlashcardResponse.builder()
                                                        .id(fc.getId())
                                                        .term(fc.getTerm())
                                                        .definition(fc.getDefinition())
                                                        .build())
                                        .collect(Collectors.toList());

                        return LessonResponse.builder()
                                        .id(lesson.getId())
                                        .lessonNumber(lesson.getLessonNumber())
                                        .title(lesson.getTitle())
                                        .summary(lesson.getSummary())
                                        .content(lesson.getContent())
                                        .estimatedReadTime(lesson.getEstimatedReadTime())
                                        .isLocked(lockedMap.getOrDefault(lesson.getId(), true))
                                        .quizId(quiz != null ? quiz.getId() : null)
                                        .quiz(questions)
                                        .flashcards(flashcards)
                                        .build();
                }).collect(Collectors.toList());

                return CourseDetailResponse.builder()
                                .id(course.getId())
                                .title(course.getTitle())
                                .category(course.getCategory())
                                .description(course.getDescription())
                                .totalLessons(lessonResponses.size())
                                .lessons(lessonResponses)
                                .build();
        }

        @Transactional(readOnly = true)
        public List<LessonProgressItem> getCourseProgress(Long courseId, Long studentId) {
                Course course = courseRepository.findByIdAndIsDeletedFalse(courseId)
                                .orElseThrow(() -> new IllegalArgumentException("Course not found."));
                if (!course.getStudentId().equals(studentId)) {
                        throw new IllegalArgumentException("Access denied.");
                }

                List<Long> lessonIds = lessonRepository
                                .findByCourseIdOrderByLessonNumberAsc(courseId)
                                .stream().map(Lesson::getId).collect(Collectors.toList());

                return lessonProgressRepository
                                .findByStudentIdAndLessonIdIn(studentId, lessonIds)
                                .stream()
                                .map(lp -> LessonProgressItem.builder()
                                                .lessonId(lp.getLessonId())
                                                .isCompleted(Boolean.TRUE.equals(lp.getIsCompleted()))
                                                .isLocked(Boolean.TRUE.equals(lp.getIsLocked()))
                                                .quizPassed(Boolean.TRUE.equals(lp.getQuizPassed()))
                                                .build())
                                .collect(Collectors.toList());
        }
}
