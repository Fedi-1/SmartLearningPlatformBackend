package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.course.QuizQuestionResponse;
import com.example.SmartLearningPlatformBackend.dto.lesson.LessonProgressResponse;
import com.example.SmartLearningPlatformBackend.dto.quiz.*;
import com.example.SmartLearningPlatformBackend.enums.DifficultyLevel;
import com.example.SmartLearningPlatformBackend.enums.FinishReason;
import com.example.SmartLearningPlatformBackend.enums.NotificationCategory;
import com.example.SmartLearningPlatformBackend.enums.QuestionType;
import com.example.SmartLearningPlatformBackend.models.*;
import com.example.SmartLearningPlatformBackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

        private final QuizRepository quizRepository;
        private final QuizAttemptRepository quizAttemptRepository;
        private final QuizAnswerRepository quizAnswerRepository;
        private final QuizQuestionRepository quizQuestionRepository;
        private final LessonRepository lessonRepository;
        private final LessonProgressRepository lessonProgressRepository;
        private final CourseRepository courseRepository;
        private final AiServiceClient aiServiceClient;
        private final NotificationService notificationService;

        // ─── Start attempt ───────────────────────────────────────────────────────

        @Transactional
        public QuizAttemptResponse startAttempt(Long quizId, Long studentId) {

                Quiz quiz = quizRepository.findById(quizId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Quiz not found."));

                int attemptsUsed = quizAttemptRepository.countByStudentIdAndQuizId(studentId, quizId);

                if (attemptsUsed >= quiz.getMaxAttempts()) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "Maximum attempts reached for this quiz.");
                }

                QuizAttempt attempt = QuizAttempt.builder()
                                .studentId(studentId)
                                .quizId(quizId)
                                .attemptNumber(attemptsUsed + 1)
                                .score(0)
                                .isPassed(false)
                                .startedAt(LocalDateTime.now())
                                .build();
                attempt = quizAttemptRepository.save(attempt);

                // ── Fetch the lesson content for this quiz ────────────────────────
                Lesson lesson = lessonRepository.findById(quiz.getLessonId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Lesson not found."));

                // ── Call FastAPI to generate 5 fresh questions on demand ──────────
                List<String> previousQuestions = quizQuestionRepository.findByQuizId(quiz.getId())
                                .stream()
                                .map(QuizQuestion::getQuestionText)
                                .collect(Collectors.toList());

                List<Map<String, Object>> rawQuestions = aiServiceClient.generateQuizQuestions(
                                lesson.getContent(), previousQuestions);

                // ── Persist generated questions and link them to this attempt ─────
                List<QuizQuestion> savedQuestions = new ArrayList<>();
                for (int i = 0; i < rawQuestions.size(); i++) {
                        Map<String, Object> q = rawQuestions.get(i);

                        QuestionType questionType = parseQuestionType(getString(q, "questionType"));
                        DifficultyLevel difficulty = parseDifficulty(getString(q, "difficulty"));
                        int pointsWorth = pointsFor(difficulty);

                        List<?> opts = q.get("options") instanceof List ? (List<?>) q.get("options") : List.of();

                        QuizQuestion question = QuizQuestion.builder()
                                        .quizId(quizId)
                                        .questionNumber(i + 1)
                                        .questionText(getString(q, "question"))
                                        .questionType(questionType)
                                        .correctAnswer(getString(q, "correctAnswer"))
                                        .option1(opts.size() > 0 ? opts.get(0).toString() : "")
                                        .option2(opts.size() > 1 ? opts.get(1).toString() : "")
                                        .option3(opts.size() > 2 ? opts.get(2).toString() : "")
                                        .option4(opts.size() > 3 ? opts.get(3).toString() : null)
                                        .explanation(getString(q, "explanation"))
                                        .difficulty(difficulty)
                                        .pointsWorth(pointsWorth)
                                        .build();

                        QuizQuestion saved = quizQuestionRepository.save(question);
                        savedQuestions.add(saved);
                }

                // Build question DTOs for response
                List<QuizQuestionResponse> questionDtos = savedQuestions.stream()
                                .map(this::toQuestionResponse)
                                .collect(Collectors.toList());

                return toAttemptResponse(attempt, attemptsUsed + 1, quiz.getMaxAttempts(), questionDtos, quiz);
        }

        // ─── Submit attempt ──────────────────────────────────────────────────────

        @Transactional
        public SubmitQuizResponse submitAttempt(Long attemptId, Long studentId, SubmitQuizRequest request) {

                QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Attempt not found."));

                if (!attempt.getStudentId().equals(studentId)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
                }
                if (attempt.getSubmittedAt() != null) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Attempt already submitted.");
                }

                Quiz quiz = quizRepository.findById(attempt.getQuizId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Quiz not found."));

                // Save answers + accumulate weighted points
                int totalPoints = 0;
                int earnedPoints = 0;

                for (AnswerRequest ans : request.getAnswers()) {
                        QuizQuestion question = quizQuestionRepository.findById(ans.getQuestionId())
                                        .orElseThrow(() -> new ResponseStatusException(
                                                        HttpStatus.BAD_REQUEST,
                                                        "Question not found: " + ans.getQuestionId()));

                        totalPoints += question.getPointsWorth();
                        boolean correct;
                        if (question.getQuestionType() == QuestionType.FILL_BLANK) {
                                correct = question.getCorrectAnswer().equalsIgnoreCase(ans.getStudentAnswer().trim());
                        } else {
                                correct = question.getCorrectAnswer().equals(ans.getStudentAnswer());
                        }
                        int pts = correct ? question.getPointsWorth() : 0;
                        earnedPoints += pts;

                        quizAnswerRepository.save(QuizAnswer.builder()
                                        .quizAttemptId(attemptId)
                                        .questionId(question.getId())
                                        .studentAnswer(ans.getStudentAnswer())
                                        .isCorrect(correct)
                                        .pointsAwarded(pts)
                                        .build());
                }

                // Calculate score
                int score = totalPoints > 0
                                ? (int) Math.round((earnedPoints * 100.0) / totalPoints)
                                : 0;

                boolean passed = score >= quiz.getPassingScore();

                attempt.setScore(score);
                attempt.setIsPassed(passed);
                attempt.setSubmittedAt(LocalDateTime.now());

                // Determine finish reason from request (defaults to SUBMITTED)
                FinishReason finishReason = FinishReason.SUBMITTED;
                if (request.getFinishReason() != null) {
                        try {
                                finishReason = FinishReason.valueOf(request.getFinishReason().toUpperCase());
                        } catch (IllegalArgumentException ignored) {
                        }
                }
                attempt.setFinishReason(finishReason);
                quizAttemptRepository.save(attempt);

                int attemptsUsed = quizAttemptRepository.countByStudentIdAndQuizId(studentId, quiz.getId());
                boolean attemptsExhausted = attemptsUsed >= quiz.getMaxAttempts();

                LessonProgressResponse progressResponse = triggerProgression(
                                studentId, quiz, passed, attemptsExhausted, attempt.getStartedAt());

                return SubmitQuizResponse.builder()
                                .attemptId(attemptId)
                                .score(score)
                                .isPassed(passed)
                                .attemptsUsed(attemptsUsed)
                                .maxAttempts(quiz.getMaxAttempts())
                                .attemptsExhausted(attemptsExhausted)
                                .lessonProgress(progressResponse)
                                .build();
        }

        // ─── Abandon attempt ─────────────────────────────────────────────────────

        @Transactional
        public QuizAttemptResponse abandonAttempt(Long attemptId, Long studentId) {

                QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Attempt not found."));

                if (!attempt.getStudentId().equals(studentId)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
                }
                if (attempt.getSubmittedAt() != null) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Attempt already submitted.");
                }

                Quiz quiz = quizRepository.findById(attempt.getQuizId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Quiz not found."));

                attempt.setScore(0);
                attempt.setIsPassed(false);
                attempt.setSubmittedAt(LocalDateTime.now());
                attempt.setFinishReason(FinishReason.ABANDONED);
                quizAttemptRepository.save(attempt);

                int attemptsUsed = quizAttemptRepository.countByStudentIdAndQuizId(studentId, quiz.getId());
                // NOTE: No lesson unlock on abandon
                return toAttemptResponse(attempt, attemptsUsed, quiz.getMaxAttempts(), null, quiz);
        }

        // ─── My attempts ─────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<QuizAttemptResponse> getMyAttempts(Long quizId, Long studentId) {

                Quiz quiz = quizRepository.findById(quizId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Quiz not found."));

                return quizAttemptRepository.findByStudentIdAndQuizId(studentId, quizId)
                                .stream()
                                .map(a -> toAttemptResponse(a, a.getAttemptNumber(), quiz.getMaxAttempts(), null))
                                .collect(Collectors.toList());
        }

        // ─── Parsing helpers ─────────────────────────────────────────────────────

        private String getString(Map<String, Object> map, String key) {
                Object val = map.get(key);
                return val != null ? val.toString() : "";
        }

        private QuestionType parseQuestionType(String raw) {
                try {
                        return QuestionType.valueOf(raw.trim().toUpperCase());
                } catch (Exception e) {
                        return QuestionType.MCQ;
                }
        }

        private DifficultyLevel parseDifficulty(String raw) {
                try {
                        return DifficultyLevel.valueOf(raw.trim().toUpperCase());
                } catch (Exception e) {
                        return DifficultyLevel.MEDIUM;
                }
        }

        private int pointsFor(DifficultyLevel level) {
                return switch (level) {
                        case EASY -> 1;
                        case MEDIUM -> 2;
                        case HARD -> 3;
                };
        }

        // ─── Lesson progression ──────────────────────────────────────────────────

        private LessonProgressResponse triggerProgression(
                        Long studentId, Quiz quiz, boolean passed, boolean attemptsExhausted,
                        LocalDateTime attemptStartedAt) {

                Lesson lesson = lessonRepository.findById(quiz.getLessonId()).orElse(null);
                if (lesson == null)
                        return null;

                LessonProgress currentProgress = lessonProgressRepository
                                .findByStudentIdAndLessonId(studentId, lesson.getId())
                                .orElse(null);
                if (currentProgress == null)
                        return null;

                if (passed) {
                        currentProgress.setIsCompleted(true);
                        currentProgress.setQuizPassed(true);
                        currentProgress.setCompletedAt(LocalDateTime.now());
                        lessonProgressRepository.save(currentProgress);

                        long minutesElapsed = ChronoUnit.MINUTES.between(
                                        attemptStartedAt, currentProgress.getCompletedAt());
                        int timeSpent = (int) Math.min(minutesElapsed, 480);
                        currentProgress.setTimeSpent(timeSpent);
                        lessonProgressRepository.save(currentProgress);

                        // Check if all lessons in the course are now complete → fire COURSE_COMPLETE
                        List<Lesson> allLessons = lessonRepository
                                        .findByCourseIdOrderByLessonNumberAsc(lesson.getCourseId());
                        List<Long> allLessonIds = allLessons.stream().map(Lesson::getId).collect(Collectors.toList());
                        List<LessonProgress> allProgress = lessonProgressRepository
                                        .findByStudentIdAndLessonIdIn(studentId, allLessonIds);
                        boolean courseComplete = allProgress.size() == allLessons.size()
                                        && allProgress.stream().allMatch(p -> Boolean.TRUE.equals(p.getIsCompleted()));
                        if (courseComplete) {
                                Course course = courseRepository.findById(lesson.getCourseId())
                                                .orElse(null);
                                String courseTitle = course != null ? course.getTitle() : "your course";
                                notificationService.notify(
                                                studentId,
                                                NotificationCategory.COURSE_COMPLETE,
                                                "Course Completed 🎓",
                                                String.format("You've completed all lessons in \"%s\". You can now take the final exam!",
                                                                courseTitle),
                                                lesson.getCourseId(),
                                                "/dashboard/courses/" + lesson.getCourseId());
                        }
                }

                if (passed || attemptsExhausted) {
                        List<Lesson> courseLessons = lessonRepository
                                        .findByCourseIdOrderByLessonNumberAsc(lesson.getCourseId());
                        courseLessons.stream()
                                        .filter(l -> l.getLessonNumber() == lesson.getLessonNumber() + 1)
                                        .findFirst()
                                        .ifPresent(next -> lessonProgressRepository
                                                        .findByStudentIdAndLessonId(studentId, next.getId())
                                                        .ifPresent(np -> {
                                                                np.setIsLocked(false);
                                                                lessonProgressRepository.save(np);
                                                        }));
                }

                return LessonProgressResponse.builder()
                                .id(currentProgress.getId())
                                .lessonId(currentProgress.getLessonId())
                                .isCompleted(Boolean.TRUE.equals(currentProgress.getIsCompleted()))
                                .isLocked(Boolean.TRUE.equals(currentProgress.getIsLocked()))
                                .quizPassed(Boolean.TRUE.equals(currentProgress.getQuizPassed()))
                                .build();
        }

        // ─── Mappers ─────────────────────────────────────────────────────────────

        private QuizAttemptResponse toAttemptResponse(
                        QuizAttempt a, int attemptsUsed, int maxAttempts,
                        List<QuizQuestionResponse> questions) {
                return toAttemptResponse(a, attemptsUsed, maxAttempts, questions, null);
        }

        private QuizAttemptResponse toAttemptResponse(
                        QuizAttempt a, int attemptsUsed, int maxAttempts,
                        List<QuizQuestionResponse> questions, Quiz quiz) {

                return QuizAttemptResponse.builder()
                                .id(a.getId())
                                .quizId(a.getQuizId())
                                .attemptNumber(a.getAttemptNumber())
                                .score(a.getScore())
                                .isPassed(Boolean.TRUE.equals(a.getIsPassed()))
                                .startedAt(a.getStartedAt())
                                .submittedAt(a.getSubmittedAt())
                                .finishReason(a.getFinishReason() != null ? a.getFinishReason().name() : null)
                                .attemptsUsed(attemptsUsed)
                                .maxAttempts(maxAttempts)
                                .timeLimitMinutes(quiz != null ? quiz.getTimeLimitMinutes() : null)
                                .questions(questions)
                                .build();
        }

        private QuizQuestionResponse toQuestionResponse(QuizQuestion q) {
                List<String> opts = new ArrayList<>();
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
                                .questionType(q.getQuestionType() != null ? q.getQuestionType().name() : "MCQ")
                                .options(opts)
                                .correctAnswer(q.getCorrectAnswer())
                                .explanation(q.getExplanation())
                                .difficulty(q.getDifficulty())
                                .build();
        }
}
