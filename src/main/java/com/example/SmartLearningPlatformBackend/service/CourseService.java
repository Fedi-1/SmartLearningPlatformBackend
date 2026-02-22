package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.ai.AiCourseResponse;
import com.example.SmartLearningPlatformBackend.dto.ai.FlashcardDto;
import com.example.SmartLearningPlatformBackend.dto.ai.LessonDto;
import com.example.SmartLearningPlatformBackend.dto.ai.QuizQuestionDto;
import com.example.SmartLearningPlatformBackend.dto.course.*;
import com.example.SmartLearningPlatformBackend.enums.DifficultyLevel;
import com.example.SmartLearningPlatformBackend.enums.QuestionType;
import com.example.SmartLearningPlatformBackend.models.*;
import com.example.SmartLearningPlatformBackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

        private final CourseRepository courseRepository;
        private final LessonRepository lessonRepository;
        private final QuizRepository quizRepository;
        private final QuizQuestionRepository quizQuestionRepository;
        private final FlashcardRepository flashcardRepository;
        private final LessonProgressRepository lessonProgressRepository;

        @Transactional
        public Course generateAndSave(AiCourseResponse aiResponse, Document document, Student student) {

                Course course = Course.builder()
                                .documentId(document.getId())
                                .studentId(student.getId())
                                .title(aiResponse.getCourseTitle())
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
                                        .build();
                        lesson = lessonRepository.save(lesson);

                        Quiz quiz = Quiz.builder()
                                        .lessonId(lesson.getId())
                                        .title("Quiz — " + lessonDto.getTitle())
                                        .passingScore(70)
                                        .maxAttempts(3)
                                        .build();
                        quiz = quizRepository.save(quiz);

                        if (lessonDto.getQuizzes() != null) {
                                int qNum = 1;
                                for (QuizQuestionDto qDto : lessonDto.getQuizzes()) {
                                        List<String> opts = qDto.getOptions() != null ? qDto.getOptions() : List.of();

                                        DifficultyLevel difficulty;
                                        try {
                                                difficulty = qDto.getDifficulty() != null
                                                                ? DifficultyLevel.valueOf(
                                                                                qDto.getDifficulty().toUpperCase())
                                                                : DifficultyLevel.MEDIUM;
                                        } catch (IllegalArgumentException e) {
                                                difficulty = DifficultyLevel.MEDIUM;
                                        }

                                        QuestionType questionType;
                                        try {
                                                questionType = qDto.getQuestionType() != null
                                                                ? QuestionType.valueOf(
                                                                                qDto.getQuestionType().toUpperCase())
                                                                : QuestionType.MCQ;
                                        } catch (IllegalArgumentException e) {
                                                questionType = QuestionType.MCQ;
                                        }

                                        // For FILL_BLANK: options is empty — store empty strings in option slots
                                        // For TRUE_FALSE: only 2 options — option3/option4 are empty
                                        int pointsWorth = difficulty == DifficultyLevel.EASY ? 1
                                                        : difficulty == DifficultyLevel.HARD ? 3
                                                                        : 2; // MEDIUM
                                        QuizQuestion question = QuizQuestion.builder()
                                                        .quizId(quiz.getId())
                                                        .questionNumber(qNum++)
                                                        .questionText(qDto.getQuestion())
                                                        .questionType(questionType)
                                                        .correctAnswer(qDto.getCorrectAnswer())
                                                        .option1(opts.size() > 0 ? opts.get(0) : "")
                                                        .option2(opts.size() > 1 ? opts.get(1) : "")
                                                        .option3(opts.size() > 2 ? opts.get(2) : "")
                                                        .option4(opts.size() > 3 ? opts.get(3) : "")
                                                        .explanation(qDto.getExplanation())
                                                        .difficulty(difficulty)
                                                        .pointsWorth(pointsWorth)
                                                        .build();
                                        quizQuestionRepository.save(question);
                                }
                        }

                        if (lessonDto.getFlashcards() != null) {
                                for (FlashcardDto fcDto : lessonDto.getFlashcards()) {
                                        Flashcard flashcard = Flashcard.builder()
                                                        .lessonId(lesson.getId())
                                                        .term(fcDto.getTerm())
                                                        .definition(fcDto.getDefinition())
                                                        .difficulty(DifficultyLevel.MEDIUM)
                                                        .build();
                                        flashcardRepository.save(flashcard);
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

                        List<QuizQuestionResponse> questions = quiz == null ? List.of()
                                        : quizQuestionRepository.findByQuizIdOrderByQuestionNumberAsc(quiz.getId())
                                                        .stream()
                                                        .map(q -> {
                                                                // Build options list, excluding blank slots for
                                                                // TRUE_FALSE / FILL_BLANK
                                                                List<String> opts = new java.util.ArrayList<>();
                                                                if (q.getOption1() != null && !q.getOption1().isEmpty())
                                                                        opts.add(q.getOption1());
                                                                if (q.getOption2() != null && !q.getOption2().isEmpty())
                                                                        opts.add(q.getOption2());
                                                                if (q.getOption3() != null && !q.getOption3().isEmpty())
                                                                        opts.add(q.getOption3());
                                                                if (q.getOption4() != null && !q.getOption4().isEmpty())
                                                                        opts.add(q.getOption4());
                                                                return QuizQuestionResponse.builder()
                                                                                .id(q.getId())
                                                                                .questionNumber(q.getQuestionNumber())
                                                                                .questionText(q.getQuestionText())
                                                                                .questionType(q.getQuestionType() != null
                                                                                                ? q.getQuestionType()
                                                                                                                .name()
                                                                                                : "MCQ")
                                                                                .options(opts)
                                                                                .correctAnswer(q.getCorrectAnswer())
                                                                                .explanation(q.getExplanation())
                                                                                .difficulty(q.getDifficulty())
                                                                                .build();
                                                        })
                                                        .collect(Collectors.toList());

                        List<FlashcardResponse> flashcards = flashcardRepository
                                        .findByLessonIdAndIsDeletedFalse(lesson.getId())
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
                                        .isLocked(lockedMap.getOrDefault(lesson.getId(), true))
                                        .quizId(quiz != null ? quiz.getId() : null)
                                        .quiz(questions)
                                        .flashcards(flashcards)
                                        .build();
                }).collect(Collectors.toList());

                return CourseDetailResponse.builder()
                                .id(course.getId())
                                .title(course.getTitle())
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
}
