package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.course.QuizQuestionResponse;
import com.example.SmartLearningPlatformBackend.dto.lesson.LessonProgressResponse;
import com.example.SmartLearningPlatformBackend.dto.quiz.*;
import com.example.SmartLearningPlatformBackend.enums.FinishReason;
import com.example.SmartLearningPlatformBackend.enums.QuestionType;
import com.example.SmartLearningPlatformBackend.models.*;
import com.example.SmartLearningPlatformBackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

        private final QuizRepository quizRepository;
        private final QuizAttemptRepository quizAttemptRepository;
        private final QuizAttemptQuestionRepository quizAttemptQuestionRepository;
        private final QuizAnswerRepository quizAnswerRepository;
        private final QuizQuestionRepository quizQuestionRepository;
        private final LessonRepository lessonRepository;
        private final LessonProgressRepository lessonProgressRepository;

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

                // ── Random selection: 2 EASY + 2 MEDIUM + 1 HARD ────────────────────
                List<QuizQuestion> selected = pickQuestions(quizId);

                // Persist selected question ids
                final Long attemptId = attempt.getId();
                selected.forEach(q -> quizAttemptQuestionRepository.save(
                                QuizAttemptQuestion.builder()
                                                .quizAttemptId(attemptId)
                                                .quizQuestionId(q.getId())
                                                .build()));

                // Build question DTOs for response
                List<QuizQuestionResponse> questionDtos = selected.stream()
                                .map(this::toQuestionResponse)
                                .collect(Collectors.toList());

                return toAttemptResponse(attempt, attemptsUsed + 1, quiz.getMaxAttempts(), questionDtos);
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

                // Load only the 5 questions selected for this attempt
                List<Long> selectedIds = quizAttemptQuestionRepository
                                .findByQuizAttemptId(attemptId)
                                .stream()
                                .map(QuizAttemptQuestion::getQuizQuestionId)
                                .collect(Collectors.toList());

                List<QuizQuestion> questions = quizQuestionRepository.findAllById(selectedIds);

                // Save answers + accumulate weighted points (only over the 5 selected
                // questions)
                int totalPoints = questions.stream().mapToInt(QuizQuestion::getPointsWorth).sum();
                int earnedPoints = 0;

                for (AnswerRequest ans : request.getAnswers()) {
                        QuizQuestion question = questions.stream()
                                        .filter(q -> q.getId().equals(ans.getQuestionId()))
                                        .findFirst()
                                        .orElseThrow(() -> new ResponseStatusException(
                                                        HttpStatus.BAD_REQUEST,
                                                        "Question not found: " + ans.getQuestionId()));

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
                attempt.setFinishReason(FinishReason.SUBMITTED);
                quizAttemptRepository.save(attempt);

                int attemptsUsed = quizAttemptRepository.countByStudentIdAndQuizId(studentId, quiz.getId());
                boolean attemptsExhausted = attemptsUsed >= quiz.getMaxAttempts();

                LessonProgressResponse progressResponse = triggerProgression(
                                studentId, quiz, passed, attemptsExhausted);

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

        // ─── Random question picker ───────────────────────────────────────────────

        private List<QuizQuestion> pickQuestions(Long quizId) {
                List<QuizQuestion> easy = shuffle(
                                quizQuestionRepository.findByQuizIdAndQuestionType(quizId, QuestionType.TRUE_FALSE));
                List<QuizQuestion> medium = shuffle(
                                quizQuestionRepository.findByQuizIdAndQuestionType(quizId, QuestionType.MCQ));
                List<QuizQuestion> hard = shuffle(
                                quizQuestionRepository.findByQuizIdAndQuestionType(quizId, QuestionType.FILL_BLANK));

                List<QuizQuestion> selected = new ArrayList<>();
                selected.addAll(easy.stream().limit(2).collect(Collectors.toList()));
                selected.addAll(medium.stream().limit(2).collect(Collectors.toList()));
                selected.addAll(hard.stream().limit(1).collect(Collectors.toList()));

                // Fallback: if pool is smaller than expected, fill from whatever is available
                if (selected.size() < 5) {
                        List<QuizQuestion> all = quizQuestionRepository.findByQuizIdOrderByQuestionNumberAsc(quizId);
                        Set<Long> pickedIds = selected.stream().map(QuizQuestion::getId).collect(Collectors.toSet());
                        shuffle(all).stream()
                                        .filter(q -> !pickedIds.contains(q.getId()))
                                        .limit(5 - selected.size())
                                        .forEach(selected::add);
                }

                // Renumber 1–5 in display order for the frontend
                for (int i = 0; i < selected.size(); i++) {
                        selected.get(i).setQuestionNumber(i + 1);
                }

                return selected;
        }

        private <T> List<T> shuffle(List<T> list) {
                List<T> copy = new ArrayList<>(list);
                Collections.shuffle(copy);
                return copy;
        }

        // ─── Lesson progression ──────────────────────────────────────────────────

        private LessonProgressResponse triggerProgression(
                        Long studentId, Quiz quiz, boolean passed, boolean attemptsExhausted) {

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
