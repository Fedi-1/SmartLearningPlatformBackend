package com.example.SmartLearningPlatformBackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.SmartLearningPlatformBackend.dto.lesson.LessonProgressResponse;
import com.example.SmartLearningPlatformBackend.enums.NotificationCategory;
import com.example.SmartLearningPlatformBackend.models.Course;
import com.example.SmartLearningPlatformBackend.models.Lesson;
import com.example.SmartLearningPlatformBackend.models.LessonProgress;
import com.example.SmartLearningPlatformBackend.models.Quiz;
import com.example.SmartLearningPlatformBackend.models.QuizAttempt;
import com.example.SmartLearningPlatformBackend.repository.CourseRepository;
import com.example.SmartLearningPlatformBackend.repository.FlashcardRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonProgressRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizAttemptRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonProgressServiceTest {

    @Mock
    private LessonProgressRepository lessonProgressRepository;
    @Mock
    private QuizAttemptRepository quizAttemptRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private FlashcardRepository flashcardRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private AiServiceClient aiServiceClient;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LessonProgressService lessonProgressService;

    @Test
    void processQuizAttempt_rejectsAttemptOwnedByAnotherStudent() {
        QuizAttempt attempt = QuizAttempt.builder().id(1L).studentId(5L).build();
        when(quizAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));

        assertThrows(IllegalArgumentException.class, () -> lessonProgressService.processQuizAttempt(1L, 99L));
    }

    @Test
    void processQuizAttempt_marksLessonCompleteAndUnlocksNextLesson() {
        QuizAttempt attempt = QuizAttempt.builder()
                .id(1L)
                .studentId(5L)
                .quizId(2L)
                .isPassed(true)
                .startedAt(LocalDateTime.now().minusMinutes(8))
                .build();
        Quiz quiz = Quiz.builder().id(2L).lessonId(10L).maxAttempts(3).build();
        Lesson currentLesson = Lesson.builder().id(10L).courseId(100L).lessonNumber(1).build();
        Lesson nextLesson = Lesson.builder().id(11L).courseId(100L).lessonNumber(2).build();
        LessonProgress currentProgress = LessonProgress.builder()
                .id(20L)
                .studentId(5L)
                .lessonId(10L)
                .isCompleted(false)
                .quizPassed(false)
                .isLocked(false)
                .build();
        LessonProgress nextProgress = LessonProgress.builder()
                .id(21L)
                .studentId(5L)
                .lessonId(11L)
                .isCompleted(false)
                .quizPassed(false)
                .isLocked(true)
                .build();

        when(quizAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
        when(quizRepository.findById(2L)).thenReturn(Optional.of(quiz));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(currentLesson));
        when(lessonProgressRepository.findByStudentIdAndLessonId(5L, 10L)).thenReturn(Optional.of(currentProgress));
        when(quizAttemptRepository.countByStudentIdAndQuizId(5L, 2L)).thenReturn(1);
        when(lessonRepository.findByCourseIdOrderByLessonNumberAsc(100L))
                .thenReturn(List.of(currentLesson, nextLesson));
        when(lessonProgressRepository.findByStudentIdAndLessonIdIn(5L, List.of(10L, 11L)))
                .thenReturn(List.of(currentProgress, nextProgress));
        when(lessonProgressRepository.findByStudentIdAndLessonId(5L, 11L)).thenReturn(Optional.of(nextProgress));

        LessonProgressResponse response = lessonProgressService.processQuizAttempt(1L, 5L);

        assertTrue(response.isCompleted());
        assertTrue(currentProgress.getQuizPassed());
        assertEquals(Boolean.FALSE, nextProgress.getIsLocked());
        verify(lessonProgressRepository, org.mockito.Mockito.atLeastOnce()).save(any(LessonProgress.class));
    }

    @Test
    void processQuizAttempt_notifiesWhenCourseIsComplete() {
        QuizAttempt attempt = QuizAttempt.builder()
                .id(1L)
                .studentId(5L)
                .quizId(2L)
                .isPassed(true)
                .startedAt(LocalDateTime.now().minusMinutes(3))
                .build();
        Quiz quiz = Quiz.builder().id(2L).lessonId(10L).maxAttempts(3).build();
        Lesson lesson = Lesson.builder().id(10L).courseId(100L).lessonNumber(1).build();
        LessonProgress progress = LessonProgress.builder()
                .id(20L)
                .studentId(5L)
                .lessonId(10L)
                .isCompleted(false)
                .quizPassed(false)
                .isLocked(false)
                .build();

        when(quizAttemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
        when(quizRepository.findById(2L)).thenReturn(Optional.of(quiz));
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(lessonProgressRepository.findByStudentIdAndLessonId(5L, 10L)).thenReturn(Optional.of(progress));
        when(quizAttemptRepository.countByStudentIdAndQuizId(5L, 2L)).thenReturn(1);
        when(lessonRepository.findByCourseIdOrderByLessonNumberAsc(100L)).thenReturn(List.of(lesson));
        when(lessonProgressRepository.findByStudentIdAndLessonIdIn(5L, List.of(10L))).thenReturn(List.of(progress));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(Course.builder().id(100L).title("Math").build()));

        lessonProgressService.processQuizAttempt(1L, 5L);

        verify(notificationService).notify(
                5L,
                NotificationCategory.COURSE_COMPLETE,
                "Course Completed \uD83C\uDF93",
                "You've completed all lessons in \"Math\". You can now take the final exam!",
                100L,
                "/dashboard/courses/100");
    }

    @Test
    void generateLessonRecap_returnsCachedPathWithoutCallingAi() {
        Lesson lesson = Lesson.builder()
                .id(10L)
                .courseId(100L)
                .lessonNumber(1)
                .title("Intro")
                .recapVideoPath("videos/intro.mp4")
                .build();

        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));

        Map<String, String> response = lessonProgressService.generateLessonRecap(10L);

        assertEquals("videos/intro.mp4", response.get("recapVideoPath"));
        org.mockito.Mockito.verifyNoInteractions(aiServiceClient);
    }

    @Test
    void generateLessonRecap_callsAiAndStoresVideoPath() {
        Lesson lesson = Lesson.builder()
                .id(10L)
                .courseId(100L)
                .lessonNumber(1)
                .title("Intro")
                .summary("Summary")
                .content("This lesson explains the concept with examples.")
                .estimatedReadTime(4)
                .build();
        Course course = Course.builder().id(100L).title("Algorithms").build();

        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(flashcardRepository.findByLessonId(10L)).thenReturn(List.of());
        when(aiServiceClient.generateLessonRecap(
                org.mockito.Mockito.eq(10L),
                org.mockito.Mockito.eq(1),
                org.mockito.Mockito.eq("Intro"),
                org.mockito.Mockito.eq(List.of()),
                org.mockito.Mockito.eq("Summary"),
                org.mockito.Mockito.eq(4),
                org.mockito.Mockito.eq("Algorithms"),
                org.mockito.Mockito.eq("en")))
                .thenReturn(Map.of("recapVideoPath", "videos/generated.mp4"));

        Map<String, String> response = lessonProgressService.generateLessonRecap(10L);

        assertEquals("videos/generated.mp4", response.get("recapVideoPath"));
        assertEquals("videos/generated.mp4", lesson.getRecapVideoPath());
        verify(lessonRepository).save(lesson);
    }
}
