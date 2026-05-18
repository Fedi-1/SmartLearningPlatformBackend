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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class QuizService {

        private static final int QUESTIONS_PER_ATTEMPT = 5;
        private static final int QUESTION_BANK_SIZE = 15;

        private final QuizRepository quizRepository;
        private final QuizAttemptRepository quizAttemptRepository;
        private final QuizAttemptQuestionRepository quizAttemptQuestionRepository;
        private final QuizAnswerRepository quizAnswerRepository;
        private final QuizQuestionRepository quizQuestionRepository;
        private final LessonRepository lessonRepository;
        private final LessonProgressRepository lessonProgressRepository;
        private final CourseRepository courseRepository;
        private final AiServiceClient aiServiceClient;
        private final NotificationService notificationService;

        @Transactional
        public QuizAttemptResponse startAttempt(Long quizId, Long studentId) {
                Quiz quiz = quizRepository.findById(quizId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Quiz not found."));

                List<QuizAttempt> previousAttempts = quizAttemptRepository.findByStudentIdAndQuizId(studentId, quizId);
                int attemptsUsed = previousAttempts.size();

                if (attemptsUsed >= quiz.getMaxAttempts()) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                        "Maximum attempts reached for this quiz.");
                }

                Lesson lesson = lessonRepository.findById(quiz.getLessonId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Lesson not found."));

                List<QuizQuestion> questionBank = ensureQuizQuestionBank(quiz, lesson.getContent());

                QuizAttempt attempt = QuizAttempt.builder()
                                .studentId(studentId)
                                .quizId(quizId)
                                .attemptNumber(attemptsUsed + 1)
                                .score(0)
                                .isPassed(false)
                                .startedAt(LocalDateTime.now())
                                .build();
                attempt = quizAttemptRepository.save(attempt);

                List<QuizQuestion> selectedQuestions = selectQuestionsForAttempt(questionBank, previousAttempts);
                for (QuizQuestion question : selectedQuestions) {
                        quizAttemptQuestionRepository.save(QuizAttemptQuestion.builder()
                                        .quizAttemptId(attempt.getId())
                                        .quizQuestionId(question.getId())
                                        .build());
                }

                List<QuizQuestionResponse> questionDtos = new ArrayList<>();
                for (int i = 0; i < selectedQuestions.size(); i++) {
                        questionDtos.add(toQuestionResponse(selectedQuestions.get(i), i + 1));
                }

                return toAttemptResponse(attempt, attemptsUsed + 1, quiz.getMaxAttempts(), questionDtos, quiz);
        }

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

                List<Long> questionIds = quizAttemptQuestionRepository.findByQuizAttemptId(attemptId)
                                .stream()
                                .map(QuizAttemptQuestion::getQuizQuestionId)
                                .collect(Collectors.toList());

                if (questionIds.isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "No questions were assigned to this attempt.");
                }

                Map<Long, QuizQuestion> assignedQuestions = quizQuestionRepository.findAllById(questionIds)
                                .stream()
                                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));

                int totalPoints = assignedQuestions.values().stream()
                                .mapToInt(QuizQuestion::getPointsWorth)
                                .sum();
                int earnedPoints = 0;
                Set<Long> answeredQuestionIds = new HashSet<>();
                List<AnswerRequest> answers = request.getAnswers() != null ? request.getAnswers() : List.of();

                for (AnswerRequest ans : answers) {
                        QuizQuestion question = assignedQuestions.get(ans.getQuestionId());
                        if (question == null) {
                                throw new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Question not assigned to this attempt: " + ans.getQuestionId());
                        }
                        if (!answeredQuestionIds.add(ans.getQuestionId())) {
                                throw new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Duplicate answer for question: " + ans.getQuestionId());
                        }

                        String studentAnswer = ans.getStudentAnswer() != null ? ans.getStudentAnswer() : "";
                        boolean correct;
                        if (question.getQuestionType() == QuestionType.FILL_BLANK) {
                                correct = question.getCorrectAnswer().equalsIgnoreCase(studentAnswer.trim());
                        } else {
                                correct = question.getCorrectAnswer().equals(studentAnswer);
                        }
                        int pts = correct ? question.getPointsWorth() : 0;
                        earnedPoints += pts;

                        quizAnswerRepository.save(QuizAnswer.builder()
                                        .quizAttemptId(attemptId)
                                        .questionId(question.getId())
                                        .studentAnswer(studentAnswer)
                                        .isCorrect(correct)
                                        .pointsAwarded(pts)
                                        .build());
                }

                int score = totalPoints > 0
                                ? (int) Math.round((earnedPoints * 100.0) / totalPoints)
                                : 0;

                boolean passed = score >= quiz.getPassingScore();

                attempt.setScore(score);
                attempt.setIsPassed(passed);
                attempt.setSubmittedAt(LocalDateTime.now());

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
                return toAttemptResponse(attempt, attemptsUsed, quiz.getMaxAttempts(), null, quiz);
        }

        @Transactional(readOnly = true)
        public List<QuizAttemptResponse> getMyAttempts(Long quizId, Long studentId) {
                Quiz quiz = quizRepository.findById(quizId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Quiz not found."));

                return quizAttemptRepository.findByStudentIdAndQuizId(studentId, quizId)
                                .stream()
                                .map(a -> toAttemptResponse(a, a.getAttemptNumber(), quiz.getMaxAttempts(), null, quiz))
                                .collect(Collectors.toList());
        }

        private List<QuizQuestion> ensureQuizQuestionBank(Quiz quiz, String lessonContent) {
                List<QuizQuestion> bank = new ArrayList<>(
                                quizQuestionRepository.findByQuizIdOrderByQuestionNumberAsc(quiz.getId()));
                int nextQuestionNumber = bank.stream()
                                .map(QuizQuestion::getQuestionNumber)
                                .filter(Objects::nonNull)
                                .max(Integer::compareTo)
                                .orElse(0) + 1;

                if (bank.size() < QUESTION_BANK_SIZE) {
                        List<String> previousQuestions = bank.stream()
                                        .map(QuizQuestion::getQuestionText)
                                        .collect(Collectors.toList());
                        List<Map<String, Object>> rawQuestions = aiServiceClient.generateQuizQuestionBank(
                                        lessonContent, previousQuestions, QUESTION_BANK_SIZE);

                        Set<String> knownQuestions = bank.stream()
                                        .map(q -> normalizeQuestionText(q.getQuestionText()))
                                        .collect(Collectors.toSet());

                        for (Map<String, Object> rawQuestion : rawQuestions) {
                                String questionText = getString(rawQuestion, "question");
                                String normalizedQuestion = normalizeQuestionText(questionText);
                                if (normalizedQuestion.isBlank() || knownQuestions.contains(normalizedQuestion)) {
                                        continue;
                                }

                                QuizQuestion saved = quizQuestionRepository.save(
                                                buildQuestion(quiz.getId(), nextQuestionNumber++, rawQuestion));
                                bank.add(saved);
                                knownQuestions.add(normalizedQuestion);

                                if (bank.size() >= QUESTION_BANK_SIZE) {
                                        break;
                                }
                        }
                }

                if (bank.size() < QUESTION_BANK_SIZE) {
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                                        "AI service could not generate a full unique quiz question bank.");
                }
                return bank;
        }

        private QuizQuestion buildQuestion(Long quizId, int questionNumber, Map<String, Object> rawQuestion) {
                QuestionType questionType = parseQuestionType(getString(rawQuestion, "questionType"));
                DifficultyLevel difficulty = parseDifficulty(getString(rawQuestion, "difficulty"));
                List<?> opts = rawQuestion.get("options") instanceof List ? (List<?>) rawQuestion.get("options")
                                : List.of();

                return QuizQuestion.builder()
                                .quizId(quizId)
                                .questionNumber(questionNumber)
                                .questionText(getString(rawQuestion, "question"))
                                .questionType(questionType)
                                .correctAnswer(getString(rawQuestion, "correctAnswer"))
                                .option1(opts.size() > 0 ? opts.get(0).toString() : "")
                                .option2(opts.size() > 1 ? opts.get(1).toString() : "")
                                .option3(opts.size() > 2 ? opts.get(2).toString() : "")
                                .option4(opts.size() > 3 ? opts.get(3).toString() : null)
                                .explanation(getString(rawQuestion, "explanation"))
                                .difficulty(difficulty)
                                .pointsWorth(pointsFor(difficulty))
                                .build();
        }

        private List<QuizQuestion> selectQuestionsForAttempt(
                        List<QuizQuestion> questionBank,
                        List<QuizAttempt> previousAttempts) {

                Set<Long> previouslyUsedQuestionIds = previousAttempts.isEmpty()
                                ? Set.of()
                                : quizAttemptQuestionRepository
                                                .findByQuizAttemptIdIn(previousAttempts.stream()
                                                                .map(QuizAttempt::getId)
                                                                .collect(Collectors.toList()))
                                                .stream()
                                                .map(QuizAttemptQuestion::getQuizQuestionId)
                                                .collect(Collectors.toSet());

                List<QuizQuestion> candidates = questionBank.stream()
                                .filter(q -> !previouslyUsedQuestionIds.contains(q.getId()))
                                .collect(Collectors.toCollection(ArrayList::new));

                if (candidates.size() < QUESTIONS_PER_ATTEMPT) {
                        candidates = new ArrayList<>(questionBank);
                }

                Collections.shuffle(candidates);
                return new ArrayList<>(candidates.subList(0, Math.min(QUESTIONS_PER_ATTEMPT, candidates.size())));
        }

        private String normalizeQuestionText(String questionText) {
                return questionText == null ? "" : questionText.trim().toLowerCase(Locale.ROOT);
        }

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
                                                "Course Completed",
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
                return toQuestionResponse(q, q.getQuestionNumber());
        }

        private QuizQuestionResponse toQuestionResponse(QuizQuestion q, int displayNumber) {
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
                                .questionNumber(displayNumber)
                                .questionText(q.getQuestionText())
                                .questionType(q.getQuestionType() != null ? q.getQuestionType().name() : "MCQ")
                                .options(opts)
                                .correctAnswer(q.getCorrectAnswer())
                                .explanation(q.getExplanation())
                                .difficulty(q.getDifficulty())
                                .build();
        }
}
