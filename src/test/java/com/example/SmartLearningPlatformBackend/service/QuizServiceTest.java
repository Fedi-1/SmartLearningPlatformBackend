package com.example.SmartLearningPlatformBackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.SmartLearningPlatformBackend.dto.quiz.AnswerRequest;
import com.example.SmartLearningPlatformBackend.dto.quiz.SubmitQuizRequest;
import com.example.SmartLearningPlatformBackend.dto.quiz.SubmitQuizResponse;
import com.example.SmartLearningPlatformBackend.enums.DifficultyLevel;
import com.example.SmartLearningPlatformBackend.enums.FinishReason;
import com.example.SmartLearningPlatformBackend.enums.QuestionType;
import com.example.SmartLearningPlatformBackend.models.Course;
import com.example.SmartLearningPlatformBackend.models.Lesson;
import com.example.SmartLearningPlatformBackend.models.LessonProgress;
import com.example.SmartLearningPlatformBackend.models.Quiz;
import com.example.SmartLearningPlatformBackend.models.QuizAttempt;
import com.example.SmartLearningPlatformBackend.models.QuizAttemptQuestion;
import com.example.SmartLearningPlatformBackend.models.QuizQuestion;
import com.example.SmartLearningPlatformBackend.repository.CourseRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonProgressRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizAnswerRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizAttemptQuestionRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizAttemptRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizQuestionRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private QuizAttemptQuestionRepository quizAttemptQuestionRepository;
    @Mock
    private QuizAnswerRepository quizAnswerRepository;
    @Mock
    private QuizQuestionRepository quizQuestionRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private AiServiceClient aiServiceClient;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private QuizService quizService;

    @Test
    void submitAttempt_scoresPassedQuizAndUnlocksNextLesson() {
        QuizAttempt attempt = QuizAttempt.builder()
                .id(1L)
                .studentId(9L)
                .quizId(2L)
                .startedAt(LocalDateTime.now().minusMinutes(12))
                .build();
        Quiz quiz = Quiz.builder().id(2L).lessonId(3L).passingScore(70).maxAttempts(3).build();
        Lesson currentLesson = Lesson.builder().id(3L).courseId(4L).lessonNumber(1).build();
        Lesson nextLesson = Lesson.builder().id(5L).courseId(4L).lessonNumber(2).build();
        LessonProgress currentProgress = LessonProgress.builder()
                .id(30L)
                .studentId(9L)
                .lessonId(3L)
                .isLocked(false)
                .isCompleted(false)
                .quizPassed(false)
                .build();
        LessonProgress nextProgress = LessonProgress.builder()
                .id(31L)
                .studentId(9L)
                .lessonId(5L)
                .isLocked(true)
                .isCompleted(false)
                .quizPassed(false)
                .build();
        QuizQuestion question = QuizQuestion.builder()
                .id(11L)
                .quizId(2L)
                .questionNumber(1)
                .questionText("2 + 2?")
                .questionType(QuestionType.MCQ)
                .correctAnswer("4")
                .difficulty(DifficultyLevel.EASY)
                .pointsWorth(10)
                .build();

        when(quizAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
        when(quizRepository.findById(2L)).thenReturn(Optional.of(quiz));
        when(quizAttemptQuestionRepository.findByQuizAttemptId(1L))
                .thenReturn(List.of(QuizAttemptQuestion.builder().quizQuestionId(11L).build()));
        when(quizQuestionRepository.findAllById(List.of(11L))).thenReturn(List.of(question));
        when(quizAttemptRepository.countByStudentIdAndQuizId(9L, 2L)).thenReturn(1);
        when(lessonRepository.findById(3L)).thenReturn(Optional.of(currentLesson));
        when(lessonProgressRepository.findByStudentIdAndLessonId(9L, 3L)).thenReturn(Optional.of(currentProgress));
        when(lessonRepository.findByCourseIdOrderByLessonNumberAsc(4L)).thenReturn(List.of(currentLesson, nextLesson));
        when(lessonProgressRepository.findByStudentIdAndLessonIdIn(9L, List.of(3L, 5L)))
                .thenReturn(List.of(currentProgress, nextProgress));
        when(lessonProgressRepository.findByStudentIdAndLessonId(9L, 5L)).thenReturn(Optional.of(nextProgress));

        AnswerRequest answer = new AnswerRequest();
        answer.setQuestionId(11L);
        answer.setStudentAnswer("4");
        SubmitQuizRequest request = new SubmitQuizRequest();
        request.setAnswers(List.of(answer));

        SubmitQuizResponse response = quizService.submitAttempt(1L, 9L, request);

        assertEquals(100, response.getScore());
        assertEquals(true, response.isPassed());
        assertTrue(currentProgress.getIsCompleted());
        assertTrue(currentProgress.getQuizPassed());
        assertEquals(Boolean.FALSE, nextProgress.getIsLocked());

        ArgumentCaptor<QuizAttempt> attemptCaptor = ArgumentCaptor.forClass(QuizAttempt.class);
        verify(quizAttemptRepository).save(attemptCaptor.capture());
        assertEquals(100, attemptCaptor.getValue().getScore());
    }

    @Test
    void submitAttempt_rejectsAttemptOwnedByAnotherStudent() {
        QuizAttempt attempt = QuizAttempt.builder().id(1L).studentId(9L).quizId(2L).build();
        when(quizAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));

        ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> quizService.submitAttempt(1L, 99L, new SubmitQuizRequest()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("Access denied.", ex.getReason());
    }

    @Test
    void submitAttempt_rejectsDuplicateAnswersForSameQuestion() {
        QuizAttempt attempt = QuizAttempt.builder().id(1L).studentId(9L).quizId(2L).build();
        Quiz quiz = Quiz.builder().id(2L).lessonId(3L).passingScore(70).maxAttempts(3).build();
        QuizQuestion question = QuizQuestion.builder()
                .id(11L)
                .quizId(2L)
                .questionNumber(1)
                .questionText("2 + 2?")
                .questionType(QuestionType.MCQ)
                .correctAnswer("4")
                .difficulty(DifficultyLevel.EASY)
                .pointsWorth(10)
                .build();
        AnswerRequest first = new AnswerRequest();
        first.setQuestionId(11L);
        first.setStudentAnswer("4");
        AnswerRequest duplicate = new AnswerRequest();
        duplicate.setQuestionId(11L);
        duplicate.setStudentAnswer("4");
        SubmitQuizRequest request = new SubmitQuizRequest();
        request.setAnswers(List.of(first, duplicate));

        when(quizAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
        when(quizRepository.findById(2L)).thenReturn(Optional.of(quiz));
        when(quizAttemptQuestionRepository.findByQuizAttemptId(1L))
                .thenReturn(List.of(QuizAttemptQuestion.builder().quizQuestionId(11L).build()));
        when(quizQuestionRepository.findAllById(List.of(11L))).thenReturn(List.of(question));

        ResponseStatusException ex = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> quizService.submitAttempt(1L, 9L, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Duplicate answer for question: 11", ex.getReason());
    }

    @Test
    void abandonAttempt_setsScoreToZeroAndFinishReasonAbandoned() {
        QuizAttempt attempt = QuizAttempt.builder().id(1L).studentId(9L).quizId(2L).build();
        Quiz quiz = Quiz.builder().id(2L).lessonId(3L).passingScore(70).maxAttempts(3).build();

        when(quizAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
        when(quizRepository.findById(2L)).thenReturn(Optional.of(quiz));
        when(quizAttemptRepository.save(attempt)).thenReturn(attempt);
        when(quizAttemptRepository.countByStudentIdAndQuizId(9L, 2L)).thenReturn(1);

        quizService.abandonAttempt(1L, 9L);

        assertEquals(0, attempt.getScore());
        assertEquals(Boolean.FALSE, attempt.getIsPassed());
        assertEquals(FinishReason.ABANDONED, attempt.getFinishReason());
    }
}
