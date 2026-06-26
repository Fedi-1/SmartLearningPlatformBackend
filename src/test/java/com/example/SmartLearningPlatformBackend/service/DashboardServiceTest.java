package com.example.SmartLearningPlatformBackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.example.SmartLearningPlatformBackend.dto.dashboard.DashboardResponse;
import com.example.SmartLearningPlatformBackend.enums.ActionType;
import com.example.SmartLearningPlatformBackend.models.ActivityLog;
import com.example.SmartLearningPlatformBackend.models.Course;
import com.example.SmartLearningPlatformBackend.models.Exam;
import com.example.SmartLearningPlatformBackend.models.Flashcard;
import com.example.SmartLearningPlatformBackend.models.FlashcardReview;
import com.example.SmartLearningPlatformBackend.models.Lesson;
import com.example.SmartLearningPlatformBackend.models.LessonProgress;
import com.example.SmartLearningPlatformBackend.models.Quiz;
import com.example.SmartLearningPlatformBackend.models.QuizAttempt;
import com.example.SmartLearningPlatformBackend.repository.ActivityLogRepository;
import com.example.SmartLearningPlatformBackend.repository.CourseRepository;
import com.example.SmartLearningPlatformBackend.repository.ExamAttemptRepository;
import com.example.SmartLearningPlatformBackend.repository.ExamRepository;
import com.example.SmartLearningPlatformBackend.repository.FlashcardRepository;
import com.example.SmartLearningPlatformBackend.repository.FlashcardReviewRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonProgressRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizAttemptRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private FlashcardRepository flashcardRepository;
    @Mock
    private FlashcardReviewRepository flashcardReviewRepository;
    @Mock
    private ExamRepository examRepository;
    @Mock
    private ExamAttemptRepository examAttemptRepository;
    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private StudySessionService studySessionService;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getDashboardData_returnsZeroStatsWhenStudentHasNoData() {
        when(courseRepository.findByStudentId(7L)).thenReturn(List.of());
        when(quizAttemptRepository.findByStudentId(7L)).thenReturn(List.of());
        when(flashcardReviewRepository.findByStudentIdAndNextReviewDateLessThanEqual(7L, LocalDate.now()))
                .thenReturn(List.of());
        when(activityLogRepository.findTop5ByUserIdOrderByTimestampDesc(7L)).thenReturn(List.of());
        when(studySessionService.getTotalStudyMinutes(7L)).thenReturn(0);

        DashboardResponse response = dashboardService.getDashboardData(7L);

        assertEquals(0, response.getStats().getTotalCourses());
        assertEquals(0, response.getStats().getTotalLessons());
        assertTrue(response.getCourses().isEmpty());
    }

    @Test
    void getDashboardData_calculatesCourseAndQuizStats() {
        Course course = Course.builder().id(1L).studentId(7L).title("Math").category("STEM").build();
        Lesson lesson1 = Lesson.builder().id(10L).courseId(1L).lessonNumber(1).build();
        Lesson lesson2 = Lesson.builder().id(11L).courseId(1L).lessonNumber(2).build();
        LessonProgress progress1 = LessonProgress.builder()
                .lessonId(10L)
                .isCompleted(true)
                .quizPassed(true)
                .lastAccessedAt(LocalDateTime.now().minusDays(1))
                .build();
        LessonProgress progress2 = LessonProgress.builder()
                .lessonId(11L)
                .isCompleted(false)
                .quizPassed(false)
                .lastAccessedAt(LocalDateTime.now())
                .build();
        QuizAttempt passedAttempt = QuizAttempt.builder().studentId(7L).score(80).isPassed(true).build();
        QuizAttempt failedAttempt = QuizAttempt.builder().studentId(7L).score(40).isPassed(false).build();
        Flashcard flashcard = Flashcard.builder().id(30L).lessonId(10L).build();
        FlashcardReview dueReview = FlashcardReview.builder()
                .id(40L)
                .studentId(7L)
                .flashcardId(30L)
                .nextReviewDate(LocalDate.now())
                .build();
        ActivityLog log = ActivityLog.builder()
                .action(ActionType.UPLOAD_DOCUMENT)
                .entityType("Document")
                .timestamp(LocalDateTime.now())
                .build();

        when(courseRepository.findByStudentId(7L)).thenReturn(List.of(course));
        when(lessonRepository.findByCourseIdOrderByLessonNumberAsc(1L)).thenReturn(List.of(lesson1, lesson2));
        when(lessonProgressRepository.findByStudentIdAndLessonIdIn(7L, List.of(10L, 11L)))
                .thenReturn(List.of(progress1, progress2));
        when(examRepository.findByCourseId(1L)).thenReturn(Optional.of(Exam.builder().id(50L).courseId(1L).build()));
        when(examAttemptRepository.existsByStudentIdAndExamIdAndIsPassed(7L, 50L, true)).thenReturn(true);
        when(quizRepository.findByLessonId(10L)).thenReturn(Optional.of(Quiz.builder().id(60L).build()));
        when(quizRepository.findByLessonId(11L)).thenReturn(Optional.empty());
        when(quizAttemptRepository.findByStudentId(7L)).thenReturn(List.of(passedAttempt, failedAttempt));
        when(flashcardReviewRepository.findByStudentIdAndNextReviewDateLessThanEqual(7L, LocalDate.now()))
                .thenReturn(List.of(dueReview));
        when(flashcardRepository.findByLessonId(10L)).thenReturn(List.of(flashcard));
        when(flashcardRepository.findByLessonId(11L)).thenReturn(List.of());
        when(activityLogRepository.findTop5ByUserIdOrderByTimestampDesc(7L)).thenReturn(List.of(log));
        when(studySessionService.getTotalStudyMinutes(7L)).thenReturn(15);

        DashboardResponse response = dashboardService.getDashboardData(7L);

        assertEquals(1, response.getStats().getTotalCourses());
        assertEquals(2, response.getStats().getTotalLessons());
        assertEquals(1, response.getStats().getCompletedLessons());
        assertEquals(2, response.getStats().getTotalQuizAttempts());
        assertEquals(60, response.getStats().getAverageQuizScore());
        assertEquals(50, response.getCourses().get(0).getProgressPercentage());
        assertTrue(response.getCourses().get(0).isExamPassed());
        assertEquals(1, response.getFlashcardsDue().get(0).getDueCount());
    }
}
