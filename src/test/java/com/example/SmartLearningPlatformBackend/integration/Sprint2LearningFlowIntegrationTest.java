package com.example.SmartLearningPlatformBackend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.SmartLearningPlatformBackend.controller.CourseController;
import com.example.SmartLearningPlatformBackend.controller.ExamController;
import com.example.SmartLearningPlatformBackend.controller.FlashcardController;
import com.example.SmartLearningPlatformBackend.controller.LessonProgressController;
import com.example.SmartLearningPlatformBackend.controller.QuizController;
import com.example.SmartLearningPlatformBackend.controller.SuspiciousActivityController;
import com.example.SmartLearningPlatformBackend.dto.course.LessonProgressItem;
import com.example.SmartLearningPlatformBackend.dto.exam.ExamAttemptResponse;
import com.example.SmartLearningPlatformBackend.dto.exam.SuspiciousActivityDTO;
import com.example.SmartLearningPlatformBackend.dto.flashcard.FlashcardReviewResponse;
import com.example.SmartLearningPlatformBackend.dto.quiz.QuizAttemptResponse;
import com.example.SmartLearningPlatformBackend.enums.SuspiciousActivityType;
import com.example.SmartLearningPlatformBackend.enums.UserRole;
import com.example.SmartLearningPlatformBackend.models.Student;
import com.example.SmartLearningPlatformBackend.models.UserDetailsImpl;
import com.example.SmartLearningPlatformBackend.service.CourseService;
import com.example.SmartLearningPlatformBackend.service.ExamGenerationService;
import com.example.SmartLearningPlatformBackend.service.FlashcardService;
import com.example.SmartLearningPlatformBackend.service.LessonProgressService;
import com.example.SmartLearningPlatformBackend.service.QuizService;
import com.example.SmartLearningPlatformBackend.service.SuspiciousActivityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class Sprint2LearningFlowIntegrationTest {

    @Mock
    private CourseService courseService;
    @Mock
    private FlashcardService flashcardService;
    @Mock
    private QuizService quizService;
    @Mock
    private ExamGenerationService examGenerationService;
    @Mock
    private SuspiciousActivityService suspiciousActivityService;
    @Mock
    private LessonProgressService lessonProgressService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Student student = Student.builder()
                .id(1L)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("ada@mail.test")
                .password("password")
                .role(UserRole.STUDENT)
                .build();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new CourseController(courseService),
                        new FlashcardController(flashcardService),
                        new QuizController(quizService),
                        new ExamController(examGenerationService),
                        new SuspiciousActivityController(suspiciousActivityService),
                        new LessonProgressController(lessonProgressService))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalResolver(new UserDetailsImpl(student)))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void getCourseProgress_returnsLessonLockAndCompletionState() throws Exception {
        when(courseService.getCourseProgress(20L, 1L)).thenReturn(List.of(
                LessonProgressItem.builder()
                        .lessonId(101L)
                        .isCompleted(true)
                        .isLocked(false)
                        .quizPassed(true)
                        .build(),
                LessonProgressItem.builder()
                        .lessonId(102L)
                        .isCompleted(false)
                        .isLocked(false)
                        .quizPassed(false)
                        .build()));

        mockMvc.perform(get("/api/courses/20/my-progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lessonId").value(101))
                .andExpect(jsonPath("$[0].isCompleted").value(true))
                .andExpect(jsonPath("$[1].isLocked").value(false));
    }

    @Test
    void reviewFlashcardForLesson_returnsUpdatedReviewState() throws Exception {
        when(flashcardService.reviewFlashcardForLesson(eq(30L), eq(40L), eq(1L), any()))
                .thenReturn(FlashcardReviewResponse.builder()
                        .id(50L)
                        .flashcardId(40L)
                        .term("Encapsulation")
                        .definition("Keeping data and behavior together")
                        .lastRating("GOOD")
                        .qualityScore(4)
                        .remainingDue(2)
                        .build());

        mockMvc.perform(post("/api/lessons/30/flashcards/40/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rating", "GOOD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flashcardId").value(40))
                .andExpect(jsonPath("$.lastRating").value("GOOD"))
                .andExpect(jsonPath("$.remainingDue").value(2));

        verify(flashcardService).reviewFlashcardForLesson(eq(30L), eq(40L), eq(1L), any());
    }

    @Test
    void startQuizAttempt_returnsAttemptRulesAndQuestions() throws Exception {
        when(quizService.startAttempt(60L, 1L)).thenReturn(QuizAttemptResponse.builder()
                .id(70L)
                .quizId(60L)
                .attemptNumber(1)
                .attemptsUsed(1)
                .maxAttempts(3)
                .timeLimitMinutes(10)
                .questions(List.of())
                .build());

        mockMvc.perform(post("/api/quizzes/60/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(70))
                .andExpect(jsonPath("$.quizId").value(60))
                .andExpect(jsonPath("$.maxAttempts").value(3));
    }

    @Test
    void startExamAttempt_returnsTimedFinalExamAttempt() throws Exception {
        when(examGenerationService.startAttempt(80L, 1L)).thenReturn(ExamAttemptResponse.builder()
                .id(90L)
                .examId(80L)
                .attemptNumber(1)
                .attemptsUsed(1)
                .maxAttempts(3)
                .timeLimitMinutes(60)
                .questions(List.of())
                .build());

        mockMvc.perform(post("/api/exams/80/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(90))
                .andExpect(jsonPath("$.examId").value(80))
                .andExpect(jsonPath("$.timeLimitMinutes").value(60));
    }

    @Test
    void logSuspiciousActivity_returnsStoredEventAndRunningTotal() throws Exception {
        when(suspiciousActivityService.logActivity(eq(90L), eq(1L), any()))
                .thenReturn(SuspiciousActivityDTO.builder()
                        .id(100L)
                        .examAttemptId(90L)
                        .activityType(SuspiciousActivityType.TAB_SWITCH)
                        .count(1)
                        .totalCount(3)
                        .build());

        mockMvc.perform(post("/api/exam-attempts/90/suspicious-activity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "activityType", "TAB_SWITCH",
                                "clientElapsedSeconds", 120))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examAttemptId").value(90))
                .andExpect(jsonPath("$.activityType").value("TAB_SWITCH"))
                .andExpect(jsonPath("$.totalCount").value(3));
    }

    @Test
    void trackLessonAccess_recordsLessonOpeningForAuthenticatedStudent() throws Exception {
        mockMvc.perform(post("/api/lessons/101/access"))
                .andExpect(status().isOk());

        verify(lessonProgressService).trackLessonAccess(101L, 1L);
    }
}
