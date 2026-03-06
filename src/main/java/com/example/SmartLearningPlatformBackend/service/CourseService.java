package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.ai.AiCourseResponse;
import com.example.SmartLearningPlatformBackend.dto.ai.FlashcardDto;
import com.example.SmartLearningPlatformBackend.dto.ai.LessonDto;
import com.example.SmartLearningPlatformBackend.dto.course.*;
import com.example.SmartLearningPlatformBackend.enums.DifficultyLevel;
import com.example.SmartLearningPlatformBackend.models.*;
import com.example.SmartLearningPlatformBackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

        private final CourseRepository courseRepository;
        private final LessonRepository lessonRepository;
        private final QuizRepository quizRepository;
        private final QuizQuestionRepository quizQuestionRepository;
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
                                .description(
                                                (aiResponse.getCourseDescription() != null
                                                                && !aiResponse.getCourseDescription().isBlank())
                                                                                ? aiResponse.getCourseDescription()
                                                                                : "Cours généré automatiquement à partir du document uploadé.")
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
                                        .timeLimitMinutes(15)
                                        .build();
                        quiz = quizRepository.save(quiz);

                        if (lessonDto.getFlashcards() != null) {
                                for (FlashcardDto fcDto : lessonDto.getFlashcards()) {
                                        DifficultyLevel fcDifficulty;
                                        try {
                                                fcDifficulty = (fcDto.getDifficulty() != null
                                                                && !fcDto.getDifficulty().isBlank())
                                                                                ? DifficultyLevel.valueOf(fcDto
                                                                                                .getDifficulty().trim()
                                                                                                .toUpperCase())
                                                                                : DifficultyLevel.MEDIUM;
                                        } catch (IllegalArgumentException e) {
                                                fcDifficulty = DifficultyLevel.MEDIUM;
                                        }
                                        Flashcard flashcard = Flashcard.builder()
                                                        .lessonId(lesson.getId())
                                                        .term(fcDto.getTerm())
                                                        .definition(fcDto.getDefinition())
                                                        .difficulty(fcDifficulty)
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
                Course course = courseRepository.findById(courseId)
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
                                        .recapVideoPath(lesson.getRecapVideoPath())
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
                Course course = courseRepository.findById(courseId)
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

        // ─── Clone course for duplicate document ─────────────────────────────────

        @Transactional
        public Course cloneCourseForStudent(Course sourceCourse, Document newDocument, Long studentId) {

                Course newCourse = Course.builder()
                                .documentId(newDocument.getId())
                                .studentId(studentId)
                                .title(sourceCourse.getTitle())
                                .category(sourceCourse.getCategory())
                                .description(sourceCourse.getDescription())
                                .build();
                newCourse = courseRepository.save(newCourse);

                List<Lesson> sourceLessons = lessonRepository
                                .findByCourseIdOrderByLessonNumberAsc(sourceCourse.getId());
                List<Lesson> savedLessons = new ArrayList<>();

                for (Lesson sourceLesson : sourceLessons) {

                        Lesson newLesson = Lesson.builder()
                                        .courseId(newCourse.getId())
                                        .lessonNumber(sourceLesson.getLessonNumber())
                                        .title(sourceLesson.getTitle())
                                        .content(sourceLesson.getContent())
                                        .summary(sourceLesson.getSummary())
                                        .estimatedReadTime(sourceLesson.getEstimatedReadTime())
                                        .build();
                        final Lesson savedNewLesson = lessonRepository.save(newLesson);
                        savedLessons.add(savedNewLesson);

                        // Clone flashcards
                        List<Flashcard> sourceFlashcards = flashcardRepository.findByLessonId(sourceLesson.getId());
                        for (Flashcard sourceFlashcard : sourceFlashcards) {
                                Flashcard newFlashcard = Flashcard.builder()
                                                .lessonId(savedNewLesson.getId())
                                                .term(sourceFlashcard.getTerm())
                                                .definition(sourceFlashcard.getDefinition())
                                                .difficulty(sourceFlashcard.getDifficulty())
                                                .build();
                                Flashcard savedFlashcard = flashcardRepository.save(newFlashcard);
                                flashcardReviewRepository.save(FlashcardReview.builder()
                                                .studentId(studentId)
                                                .flashcardId(savedFlashcard.getId())
                                                .easeFactor(2.5f)
                                                .interval(0)
                                                .repetitionCount(0)
                                                .consecutiveCorrectReviews(0)
                                                .nextReviewDate(LocalDate.now())
                                                .build());
                        }

                        // Clone quiz and its questions
                        quizRepository.findByLessonId(sourceLesson.getId()).ifPresent(sourceQuiz -> {
                                Quiz newQuiz = Quiz.builder()
                                                .lessonId(savedNewLesson.getId())
                                                .title(sourceQuiz.getTitle())
                                                .passingScore(sourceQuiz.getPassingScore())
                                                .maxAttempts(sourceQuiz.getMaxAttempts())
                                                .timeLimitMinutes(sourceQuiz.getTimeLimitMinutes())
                                                .build();
                                Quiz savedQuiz = quizRepository.save(newQuiz);

                                List<QuizQuestion> sourceQuestions = quizQuestionRepository
                                                .findByQuizIdOrderByQuestionNumberAsc(sourceQuiz.getId());
                                for (QuizQuestion sourceQuestion : sourceQuestions) {
                                        quizQuestionRepository.save(QuizQuestion.builder()
                                                        .quizId(savedQuiz.getId())
                                                        .questionNumber(sourceQuestion.getQuestionNumber())
                                                        .questionText(sourceQuestion.getQuestionText())
                                                        .questionType(sourceQuestion.getQuestionType())
                                                        .correctAnswer(sourceQuestion.getCorrectAnswer())
                                                        .option1(sourceQuestion.getOption1())
                                                        .option2(sourceQuestion.getOption2())
                                                        .option3(sourceQuestion.getOption3())
                                                        .option4(sourceQuestion.getOption4())
                                                        .explanation(sourceQuestion.getExplanation())
                                                        .difficulty(sourceQuestion.getDifficulty())
                                                        .pointsWorth(sourceQuestion.getPointsWorth())
                                                        .build());
                                }
                        });
                }

                // Initialise LessonProgress: first lesson unlocked, all others locked
                for (int i = 0; i < savedLessons.size(); i++) {
                        Lesson saved = savedLessons.get(i);
                        lessonProgressRepository.save(LessonProgress.builder()
                                        .studentId(studentId)
                                        .lessonId(saved.getId())
                                        .isLocked(i != 0)
                                        .isCompleted(false)
                                        .quizPassed(false)
                                        .build());
                }

                return newCourse;
        }
}
