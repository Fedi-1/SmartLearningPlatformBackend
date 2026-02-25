package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.exam.*;
import com.example.SmartLearningPlatformBackend.enums.DifficultyLevel;
import com.example.SmartLearningPlatformBackend.enums.FinishReason;
import com.example.SmartLearningPlatformBackend.enums.QuestionType;
import com.example.SmartLearningPlatformBackend.models.*;
import com.example.SmartLearningPlatformBackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamGenerationService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final ExamAttemptQuestionRepository examAttemptQuestionRepository;
    private final ExamAnswerRepository examAnswerRepository;
    private final CertificateRepository certificateRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final AiServiceClient aiServiceClient;
    private final PlatformTransactionManager transactionManager;

    // ─── Generate exam for course ─────────────────────────────────────────────

    // Public entry point — AI call happens OUTSIDE any transaction to avoid
    // timeouts
    public ExamResponse generateExamForCourse(Long courseId, Long studentId) {

        // Verify student owns this course (read-only check, no transaction needed)
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));
        if (!course.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }

        // Return existing exam if one already exists
        Optional<Exam> existing = examRepository.findByCourseId(courseId);
        if (existing.isPresent()) {
            return toExamResponse(existing.get());
        }

        // Load all lesson contents ordered by lesson number
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByLessonNumberAsc(courseId);
        if (lessons.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Course has no lessons.");
        }

        List<String> lessonContents = lessons.stream()
                .map(Lesson::getContent)
                .collect(Collectors.toList());

        // Call FastAPI — can take 30–60 s, must NOT be inside a DB transaction
        List<Map<String, Object>> rawQuestions = aiServiceClient.generateExamQuestions(
                lessonContents, course.getTitle());

        if (rawQuestions == null || rawQuestions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI service returned no questions.");
        }

        // Persist everything in a dedicated programmatic transaction (no self-call
        // proxy needed)
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        return tx.execute(status -> {
            // Guard: another request might have saved an exam while AI was running
            Optional<Exam> race = examRepository.findByCourseId(courseId);
            if (race.isPresent()) {
                return toExamResponse(race.get());
            }

            Exam exam = Exam.builder()
                    .courseId(courseId)
                    .title("Examen Final")
                    .passingScore(70)
                    .maxAttempts(3)
                    .totalPoints(45)
                    .sectionEasyCount(10)
                    .sectionMediumCount(10)
                    .sectionHardCount(5)
                    .isDeleted(false)
                    .build();
            Exam savedExam = examRepository.saveAndFlush(exam);

            for (int i = 0; i < rawQuestions.size(); i++) {
                Map<String, Object> q = rawQuestions.get(i);
                examQuestionRepository.save(ExamQuestion.builder()
                        .examId(savedExam.getId())
                        .questionNumber(i + 1)
                        .questionText(getString(q, "questionText"))
                        .questionType(parseQuestionType(getString(q, "questionType")))
                        .difficulty(parseDifficulty(getString(q, "difficulty")))
                        .sectionNumber(getInt(q, "sectionNumber", 1))
                        .pointsWorth(getInt(q, "pointsWorth", 1))
                        .correctAnswer(getString(q, "correctAnswer"))
                        .explanation(getString(q, "explanation"))
                        .option1(getStringOrNull(q, "option1"))
                        .option2(getStringOrNull(q, "option2"))
                        .option3(getStringOrNull(q, "option3"))
                        .option4(getStringOrNull(q, "option4"))
                        .build());
            }
            return toExamResponse(savedExam);
        });
    }

    // ─── Get exam for course ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ExamResponse getExamForCourse(Long courseId, Long studentId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));
        if (!course.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
        Exam exam = examRepository.findByCourseId(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No exam generated yet."));
        return toExamResponse(exam);
    }

    // ─── Start exam attempt ───────────────────────────────────────────────────

    @Transactional
    public ExamAttemptResponse startAttempt(Long examId, Long studentId) {

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found."));

        // Verify student owns this exam through course
        Course course = courseRepository.findById(exam.getCourseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found."));
        if (!course.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }

        int attemptsUsed = examAttemptRepository.countByStudentIdAndExamId(studentId, examId);
        if (attemptsUsed >= exam.getMaxAttempts()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Maximum attempts reached.");
        }

        ExamAttempt attempt = ExamAttempt.builder()
                .studentId(studentId)
                .examId(examId)
                .attemptNumber(attemptsUsed + 1)
                .isPassed(false)
                .startedAt(LocalDateTime.now())
                .build();
        attempt = examAttemptRepository.save(attempt);
        final Long attemptId = attempt.getId();

        // Randomly select 25 questions: 10 from section1, 10 from section2, 5 from
        // section3
        List<ExamQuestion> section1 = examQuestionRepository.findByExamIdAndSectionNumber(examId, 1);
        List<ExamQuestion> section2 = examQuestionRepository.findByExamIdAndSectionNumber(examId, 2);
        List<ExamQuestion> section3 = examQuestionRepository.findByExamIdAndSectionNumber(examId, 3);

        Collections.shuffle(section1);
        Collections.shuffle(section2);
        Collections.shuffle(section3);

        List<ExamQuestion> selected = new ArrayList<>();
        selected.addAll(section1.subList(0, Math.min(10, section1.size())));
        selected.addAll(section2.subList(0, Math.min(10, section2.size())));
        selected.addAll(section3.subList(0, Math.min(5, section3.size())));

        // Save selected questions to exam_attempt_questions
        for (ExamQuestion q : selected) {
            examAttemptQuestionRepository.save(ExamAttemptQuestion.builder()
                    .examAttemptId(attemptId)
                    .examQuestionId(q.getId())
                    .build());
        }

        // Renumber for display (1–25)
        List<ExamQuestionResponse> questionDtos = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            questionDtos.add(toQuestionResponse(selected.get(i), i + 1));
        }

        return ExamAttemptResponse.builder()
                .id(attempt.getId())
                .examId(examId)
                .attemptNumber(attempt.getAttemptNumber())
                .score(null)
                .isPassed(false)
                .startedAt(attempt.getStartedAt())
                .submittedAt(null)
                .finishReason(null)
                .attemptsUsed(attemptsUsed + 1)
                .maxAttempts(exam.getMaxAttempts())
                .hasCertificate(false)
                .questions(questionDtos)
                .build();
    }

    // ─── Submit exam attempt ──────────────────────────────────────────────────

    @Transactional
    public SubmitExamResponse submitAttempt(Long attemptId, Long studentId, SubmitExamRequest request) {

        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found."));
        if (!attempt.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
        if (attempt.getSubmittedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attempt already submitted.");
        }

        Exam exam = examRepository.findById(attempt.getExamId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found."));

        // Load the 25 questions selected for this attempt
        List<Long> questionIds = examAttemptQuestionRepository.findByExamAttemptId(attemptId)
                .stream()
                .map(ExamAttemptQuestion::getExamQuestionId)
                .collect(Collectors.toList());
        List<ExamQuestion> questions = examQuestionRepository.findAllById(questionIds);

        int totalPointsPossible = questions.stream().mapToInt(ExamQuestion::getPointsWorth).sum();
        int totalPointsEarned = 0;

        for (ExamAnswerRequest ans : request.getAnswers()) {
            ExamQuestion question = questions.stream()
                    .filter(q -> q.getId().equals(ans.getQuestionId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Question not found: " + ans.getQuestionId()));

            boolean correct;
            if (question.getQuestionType() == QuestionType.FILL_BLANK) {
                correct = question.getCorrectAnswer().equalsIgnoreCase(
                        ans.getStudentAnswer() != null ? ans.getStudentAnswer().trim() : "");
            } else {
                correct = question.getCorrectAnswer().equals(ans.getStudentAnswer());
            }
            int pts = correct ? question.getPointsWorth() : 0;
            totalPointsEarned += pts;

            examAnswerRepository.save(ExamAnswer.builder()
                    .examAttemptId(attemptId)
                    .questionId(question.getId())
                    .studentAnswer(ans.getStudentAnswer() != null ? ans.getStudentAnswer() : "")
                    .isCorrect(correct)
                    .pointsAwarded(pts)
                    .build());
        }

        int score = totalPointsPossible > 0
                ? (int) Math.round((totalPointsEarned * 100.0) / totalPointsPossible)
                : 0;
        boolean passed = score >= exam.getPassingScore();

        attempt.setScore(score);
        attempt.setIsPassed(passed);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setFinishReason(FinishReason.SUBMITTED);
        examAttemptRepository.save(attempt);

        // Issue certificate if passed
        String certUuid = null;
        if (passed) {
            certUuid = UUID.randomUUID().toString();
            certificateRepository.save(Certificate.builder()
                    .certificateUuid(certUuid)
                    .studentId(studentId)
                    .courseId(exam.getCourseId())
                    .examAttemptId(attemptId)
                    .score(score)
                    .issuedAt(LocalDateTime.now())
                    .isRevoked(false)
                    .pdfFilePath(null)
                    .build());
        }

        return SubmitExamResponse.builder()
                .attemptId(attemptId)
                .score(score)
                .isPassed(passed)
                .totalPointsEarned(totalPointsEarned)
                .totalPointsPossible(totalPointsPossible)
                .attemptNumber(attempt.getAttemptNumber())
                .certificateUuid(certUuid)
                .build();
    }

    // ─── My attempts ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ExamAttemptResponse> getMyAttempts(Long examId, Long studentId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found."));

        return examAttemptRepository.findByStudentIdAndExamId(studentId, examId)
                .stream()
                .map(a -> ExamAttemptResponse.builder()
                        .id(a.getId())
                        .examId(examId)
                        .attemptNumber(a.getAttemptNumber())
                        .score(a.getScore())
                        .isPassed(Boolean.TRUE.equals(a.getIsPassed()))
                        .startedAt(a.getStartedAt())
                        .submittedAt(a.getSubmittedAt())
                        .finishReason(a.getFinishReason() != null ? a.getFinishReason().name() : null)
                        .attemptsUsed(a.getAttemptNumber())
                        .maxAttempts(exam.getMaxAttempts())
                        .hasCertificate(certificateRepository.existsByExamAttemptId(a.getId()))
                        .questions(null)
                        .build())
                .collect(Collectors.toList());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ExamResponse toExamResponse(Exam exam) {
        return ExamResponse.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .passingScore(exam.getPassingScore())
                .maxAttempts(exam.getMaxAttempts())
                .totalPoints(exam.getTotalPoints())
                .sectionEasyCount(exam.getSectionEasyCount())
                .sectionMediumCount(exam.getSectionMediumCount())
                .sectionHardCount(exam.getSectionHardCount())
                .createdAt(exam.getCreatedAt())
                .build();
    }

    private ExamQuestionResponse toQuestionResponse(ExamQuestion q, int displayNumber) {
        return ExamQuestionResponse.builder()
                .id(q.getId())
                .questionNumber(displayNumber)
                .questionText(q.getQuestionText())
                .questionType(q.getQuestionType() != null ? q.getQuestionType().name() : "MCQ")
                .option1(q.getOption1())
                .option2(q.getOption2())
                .option3(q.getOption3())
                .option4(q.getOption4())
                .explanation(null) // hidden during active attempt
                .difficulty(q.getDifficulty() != null ? q.getDifficulty().name() : "MEDIUM")
                .sectionNumber(q.getSectionNumber())
                .pointsWorth(q.getPointsWorth())
                .build();
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private String getStringOrNull(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null || val.toString().equalsIgnoreCase("null") || val.toString().isBlank())
            return null;
        return val.toString();
    }

    private int getInt(Map<String, Object> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val == null)
            return defaultVal;
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return defaultVal;
        }
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
}
