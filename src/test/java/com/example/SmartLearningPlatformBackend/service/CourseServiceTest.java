package com.example.SmartLearningPlatformBackend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.SmartLearningPlatformBackend.dto.ai.AiCourseResponse;
import com.example.SmartLearningPlatformBackend.dto.ai.FlashcardDto;
import com.example.SmartLearningPlatformBackend.dto.ai.LessonDto;
import com.example.SmartLearningPlatformBackend.models.Course;
import com.example.SmartLearningPlatformBackend.models.Document;
import com.example.SmartLearningPlatformBackend.models.Flashcard;
import com.example.SmartLearningPlatformBackend.models.Lesson;
import com.example.SmartLearningPlatformBackend.models.LessonProgress;
import com.example.SmartLearningPlatformBackend.models.Quiz;
import com.example.SmartLearningPlatformBackend.models.Student;
import com.example.SmartLearningPlatformBackend.repository.CourseRepository;
import com.example.SmartLearningPlatformBackend.repository.FlashcardRepository;
import com.example.SmartLearningPlatformBackend.repository.FlashcardReviewRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonProgressRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizQuestionRepository;
import com.example.SmartLearningPlatformBackend.repository.QuizRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private QuizRepository quizRepository;
    @Mock
    private QuizQuestionRepository quizQuestionRepository;
    @Mock
    private FlashcardRepository flashcardRepository;
    @Mock
    private FlashcardReviewRepository flashcardReviewRepository;
    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void generateAndSave_initializesOnlyFirstLessonUnlocked() {
        AiCourseResponse aiResponse = new AiCourseResponse();
        aiResponse.setCourseTitle("Algebra Basics");
        aiResponse.setCategory("Mathematics");
        aiResponse.setLessons(List.of(lessonDto(1, "Intro"), lessonDto(2, "Equations")));

        Document document = Document.builder().id(10L).build();
        Student student = new Student();
        student.setId(5L);

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            course.setId(100L);
            return course;
        });
        when(lessonRepository.save(any(Lesson.class))).thenAnswer(invocation -> {
            Lesson lesson = invocation.getArgument(0);
            lesson.setId(lesson.getLessonNumber().longValue());
            return lesson;
        });
        when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(flashcardRepository.save(any(Flashcard.class))).thenAnswer(invocation -> {
            Flashcard flashcard = invocation.getArgument(0);
            flashcard.setId(200L);
            return flashcard;
        });

        courseService.generateAndSave(aiResponse, document, student);

        ArgumentCaptor<LessonProgress> progressCaptor = ArgumentCaptor.forClass(LessonProgress.class);
        verify(lessonProgressRepository, org.mockito.Mockito.times(2)).save(progressCaptor.capture());

        List<LessonProgress> savedProgress = progressCaptor.getAllValues();
        assertFalse(savedProgress.get(0).getIsLocked());
        assertTrue(savedProgress.get(1).getIsLocked());
        assertFalse(savedProgress.get(0).getIsCompleted());
        assertFalse(savedProgress.get(1).getQuizPassed());
    }

    private LessonDto lessonDto(int lessonNumber, String title) {
        FlashcardDto flashcard = new FlashcardDto();
        flashcard.setTerm(title + " term");
        flashcard.setDefinition(title + " definition");
        flashcard.setDifficulty("easy");

        LessonDto lesson = new LessonDto();
        lesson.setLessonNumber(lessonNumber);
        lesson.setTitle(title);
        lesson.setSummary(title + " summary");
        lesson.setContent(title + " content");
        lesson.setEstimatedReadTime(5);
        lesson.setFlashcards(List.of(flashcard));
        return lesson;
    }
}
