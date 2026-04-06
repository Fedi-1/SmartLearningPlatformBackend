package com.example.SmartLearningPlatformBackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.SmartLearningPlatformBackend.dto.exam.ExamAnswerRequest;
import com.example.SmartLearningPlatformBackend.dto.exam.SubmitExamRequest;
import com.example.SmartLearningPlatformBackend.enums.CertificateStatus;
import com.example.SmartLearningPlatformBackend.enums.QuestionType;
import com.example.SmartLearningPlatformBackend.models.Certificate;
import com.example.SmartLearningPlatformBackend.models.Course;
import com.example.SmartLearningPlatformBackend.models.Exam;
import com.example.SmartLearningPlatformBackend.models.ExamAttempt;
import com.example.SmartLearningPlatformBackend.models.ExamAttemptQuestion;
import com.example.SmartLearningPlatformBackend.models.ExamQuestion;
import com.example.SmartLearningPlatformBackend.repository.CertificateRepository;
import com.example.SmartLearningPlatformBackend.repository.CourseRepository;
import com.example.SmartLearningPlatformBackend.repository.ExamAnswerRepository;
import com.example.SmartLearningPlatformBackend.repository.ExamAttemptQuestionRepository;
import com.example.SmartLearningPlatformBackend.repository.ExamAttemptRepository;
import com.example.SmartLearningPlatformBackend.repository.ExamQuestionRepository;
import com.example.SmartLearningPlatformBackend.repository.ExamRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ExamGenerationServiceTest {

        @Mock
        private ExamRepository examRepository;
        @Mock
        private ExamQuestionRepository examQuestionRepository;
        @Mock
        private ExamAttemptRepository examAttemptRepository;
        @Mock
        private ExamAttemptQuestionRepository examAttemptQuestionRepository;
        @Mock
        private ExamAnswerRepository examAnswerRepository;
        @Mock
        private CourseRepository courseRepository;
        @Mock
        private LessonRepository lessonRepository;
        @Mock
        private CertificateRepository certificateRepository;
        @Mock
        private AiServiceClient aiServiceClient;
        @Mock
        private ActivityLogService activityLogService;
        @Mock
        private NotificationService notificationService;

        @InjectMocks
        private ExamGenerationService examGenerationService;

        @Test
        void submitAttempt_throwsWhenAlreadySubmitted() {
                ExamAttempt attempt = ExamAttempt.builder()
                                .id(1L)
                                .studentId(1L)
                                .submittedAt(LocalDateTime.now())
                                .build();

                when(examAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> examGenerationService.submitAttempt(1L, 1L, new SubmitExamRequest()));
                assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
                assertEquals("Attempt already submitted.", ex.getReason());
        }

        @Test
        void submitAttempt_createsCertificateWhenPassed() {
                ExamAttempt attempt = ExamAttempt.builder()
                                .id(1L)
                                .studentId(1L)
                                .examId(1L)
                                .submittedAt(null)
                                .build();

                Exam exam = Exam.builder()
                                .id(1L)
                                .courseId(1L)
                                .passingScore(50) // 50%
                                .maxAttempts(3)
                                .build();

                Course course = Course.builder()
                                .id(1L)
                                .studentId(1L)
                                .build();

                ExamAttemptQuestion eaq = ExamAttemptQuestion.builder().examQuestionId(1L).build();
                ExamQuestion question = ExamQuestion.builder()
                                .id(1L)
                                .pointsWorth(10)
                                .correctAnswer("A")
                                .questionType(QuestionType.MCQ)
                                .build();

                when(examAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
                when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
                when(examAttemptQuestionRepository.findByExamAttemptId(1L)).thenReturn(List.of(eaq));
                when(examQuestionRepository.findAllById(List.of(1L))).thenReturn(List.of(question));
                when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
                when(certificateRepository.findByStudentIdAndCourseId(1L, 1L)).thenReturn(Optional.empty());

                when(certificateRepository.save(any(Certificate.class))).thenAnswer(i -> {
                        Certificate c = i.getArgument(0);
                        c.setId(100L);
                        return c;
                });

                ExamAnswerRequest ansReq = new ExamAnswerRequest();
                ansReq.setQuestionId(1L);
                ansReq.setStudentAnswer("A"); // Correct -> 10/10 = 100% (Pass)

                SubmitExamRequest request = new SubmitExamRequest();
                request.setAnswers(List.of(ansReq));

                examGenerationService.submitAttempt(1L, 1L, request);

                ArgumentCaptor<Certificate> certCaptor = ArgumentCaptor.forClass(Certificate.class);
                verify(certificateRepository).save(certCaptor.capture());
                Certificate savedCert = certCaptor.getValue();
                assertEquals(CertificateStatus.PENDING, savedCert.getStatus());
                assertEquals(1L, savedCert.getStudentId());
                assertEquals(1L, savedCert.getCourseId());
        }

        @Test
        void submitAttempt_doesNotCreateCertificateWhenFailed() {
                ExamAttempt attempt = ExamAttempt.builder()
                                .id(1L)
                                .studentId(1L)
                                .examId(1L)
                                .submittedAt(null)
                                .build();

                Exam exam = Exam.builder()
                                .id(1L)
                                .courseId(1L)
                                .passingScore(50) // 50%
                                .maxAttempts(3)
                                .build();

                Course course = Course.builder()
                                .id(1L)
                                .studentId(1L)
                                .build();

                ExamAttemptQuestion eaq = ExamAttemptQuestion.builder().examQuestionId(1L).build();
                ExamQuestion question = ExamQuestion.builder()
                                .id(1L)
                                .pointsWorth(10)
                                .correctAnswer("A")
                                .questionType(QuestionType.MCQ)
                                .build();

                when(examAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
                when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
                when(examAttemptQuestionRepository.findByExamAttemptId(1L)).thenReturn(List.of(eaq));
                when(examQuestionRepository.findAllById(List.of(1L))).thenReturn(List.of(question));
                when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

                ExamAnswerRequest ansReq = new ExamAnswerRequest();
                ansReq.setQuestionId(1L);
                ansReq.setStudentAnswer("B"); // Incorrect -> 0/10 = 0% (Fail)

                SubmitExamRequest request = new SubmitExamRequest();
                request.setAnswers(List.of(ansReq));

                examGenerationService.submitAttempt(1L, 1L, request);

                verify(certificateRepository, never()).save(any(Certificate.class));
        }
}
