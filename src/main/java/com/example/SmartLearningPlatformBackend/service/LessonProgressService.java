package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.lesson.LessonProgressResponse;
import com.example.SmartLearningPlatformBackend.enums.NotificationCategory;
import com.example.SmartLearningPlatformBackend.models.Course;
import com.example.SmartLearningPlatformBackend.models.Flashcard;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonProgressService {

        private final LessonProgressRepository lessonProgressRepository;
        private final QuizAttemptRepository quizAttemptRepository;
        private final QuizRepository quizRepository;
        private final LessonRepository lessonRepository;
        private final FlashcardRepository flashcardRepository;
        private final CourseRepository courseRepository;
        private final AiServiceClient aiServiceClient;
        private final NotificationService notificationService;

        /**
         * Called after a quiz attempt is submitted.
         * - If attempt is passed → mark lesson complete + quizPassed, then unlock next
         * lesson.
         * - If attempts exhausted → unlock next lesson regardless (student can still
         * move on).
         */
        @Transactional
        public LessonProgressResponse processQuizAttempt(Long quizAttemptId, Long studentId) {

                QuizAttempt attempt = quizAttemptRepository.findById(quizAttemptId)
                                .orElseThrow(() -> new IllegalArgumentException("Quiz attempt not found."));

                if (!attempt.getStudentId().equals(studentId)) {
                        throw new IllegalArgumentException("Access denied.");
                }

                Quiz quiz = quizRepository.findById(attempt.getQuizId())
                                .orElseThrow(() -> new IllegalArgumentException("Quiz not found."));

                Lesson currentLesson = lessonRepository.findById(quiz.getLessonId())
                                .orElseThrow(() -> new IllegalArgumentException("Lesson not found."));

                LessonProgress currentProgress = lessonProgressRepository
                                .findByStudentIdAndLessonId(studentId, currentLesson.getId())
                                .orElseThrow(() -> new IllegalArgumentException("Lesson progress not found."));

                boolean passed = Boolean.TRUE.equals(attempt.getIsPassed());
                int attemptsUsed = quizAttemptRepository.countByStudentIdAndQuizId(studentId, quiz.getId());
                boolean attemptsExhausted = attemptsUsed >= quiz.getMaxAttempts();

                // Update current lesson progress if passed
                if (passed) {
                        currentProgress.setIsCompleted(true);
                        currentProgress.setQuizPassed(true);
                        currentProgress.setCompletedAt(LocalDateTime.now());
                        lessonProgressRepository.save(currentProgress);

                        long minutesElapsed = ChronoUnit.MINUTES.between(
                                        currentLesson.getCreatedAt(), currentProgress.getCompletedAt());
                        int timeSpent = (int) Math.min(minutesElapsed, 480);
                        currentProgress.setTimeSpent(timeSpent);
                        lessonProgressRepository.save(currentProgress);

                        // Check if all lessons in the course are now complete → fire COURSE_COMPLETE
                        List<Lesson> allLessons = lessonRepository
                                        .findByCourseIdOrderByLessonNumberAsc(currentLesson.getCourseId());
                        List<Long> allLessonIds = allLessons.stream().map(Lesson::getId).collect(Collectors.toList());
                        List<LessonProgress> allProgress = lessonProgressRepository
                                        .findByStudentIdAndLessonIdIn(studentId, allLessonIds);
                        boolean courseComplete = allProgress.size() == allLessons.size()
                                        && allProgress.stream().allMatch(p -> Boolean.TRUE.equals(p.getIsCompleted()));
                        if (courseComplete) {
                                Course course = courseRepository.findById(currentLesson.getCourseId())
                                                .orElse(null);
                                String courseTitle = course != null ? course.getTitle() : "your course";
                                notificationService.notify(
                                                studentId,
                                                NotificationCategory.COURSE_COMPLETE,
                                                "Course Completed 🎓",
                                                String.format("You've completed all lessons in \"%s\". You can now take the final exam!",
                                                                courseTitle),
                                                currentLesson.getCourseId(),
                                                "/dashboard/courses/" + currentLesson.getCourseId());
                        }
                }

                // Unlock next lesson if passed OR if all attempts are exhausted
                if (passed || attemptsExhausted) {
                        unlockNextLesson(studentId, currentLesson);
                }

                return toResponse(currentProgress);
        }

        /**
         * Called when a student opens a lesson. Updates last_accessed_at.
         */
        @Transactional
        public void trackLessonAccess(Long lessonId, Long studentId) {
                lessonProgressRepository.findByStudentIdAndLessonId(studentId, lessonId)
                                .ifPresentOrElse(progress -> {
                                        progress.setLastAccessedAt(LocalDateTime.now());
                                        lessonProgressRepository.save(progress);
                                }, () -> log.warn("No LessonProgress found for studentId={} lessonId={}", studentId,
                                                lessonId));
        }

        /**
         * Returns the LessonProgress for a given lesson and student.
         */
        @Transactional(readOnly = true)
        public LessonProgressResponse getLessonProgress(Long lessonId, Long studentId) {

                LessonProgress progress = lessonProgressRepository
                                .findByStudentIdAndLessonId(studentId, lessonId)
                                .orElseThrow(() -> new IllegalArgumentException("Lesson progress not found."));

                return toResponse(progress);
        }

        // -------------------------------------------------------------------------

        /**
         * Generates (or returns cached) a recap video for a lesson.
         * - If recapVideoPath is already set on the lesson, returns it immediately.
         * - Otherwise calls the AI service, saves the path, and returns it.
         */
        @Transactional
        public Map<String, String> generateLessonRecap(Long lessonId) {

                Lesson lesson = lessonRepository.findById(lessonId)
                                .orElseThrow(() -> new IllegalArgumentException("Lesson not found."));

                // Return cached path if already generated
                if (lesson.getRecapVideoPath() != null && !lesson.getRecapVideoPath().isBlank()) {
                        return Map.of("recapVideoPath", lesson.getRecapVideoPath());
                }

                // Load course for title
                Course course = courseRepository.findById(lesson.getCourseId())
                                .orElseThrow(() -> new IllegalArgumentException("Course not found."));

                // Load flashcard terms (up to 6)
                List<Flashcard> flashcards = flashcardRepository.findByLessonId(lessonId);
                List<String> flashcardTerms = flashcards.stream()
                                .limit(6)
                                .map(Flashcard::getTerm)
                                .collect(Collectors.toList());

                // Detect language from first 100 chars of content
                String contentPreview = lesson.getContent() != null
                                ? lesson.getContent().substring(0, Math.min(100, lesson.getContent().length()))
                                : "";
                boolean isFrench = contentPreview.matches(
                                ".*\\b(le|la|les|un|une|des|et|est|en|de|du|pour|avec|sur|dans|qui|que|ce|se|ne|pas|plus|aussi|comme|il|elle|nous|vous|ils|elles)\\b.*");
                String language = isFrench ? "fr" : "en";

                int estimatedReadTime = lesson.getEstimatedReadTime() != null
                                ? lesson.getEstimatedReadTime()
                                : Math.max(1, (int) Math.ceil(
                                                (lesson.getContent() != null ? lesson.getContent().split("\\s+").length
                                                                : 200) / 200.0));

                // Call AI service
                Map<String, String> result = aiServiceClient.generateLessonRecap(
                                lessonId,
                                lesson.getLessonNumber(),
                                lesson.getTitle(),
                                flashcardTerms,
                                lesson.getSummary() != null ? lesson.getSummary() : "",
                                estimatedReadTime,
                                course.getTitle(),
                                language);

                String videoPath = result.get("recapVideoPath");
                if (videoPath != null && !videoPath.isBlank()) {
                        lesson.setRecapVideoPath(videoPath);
                        lessonRepository.save(lesson);
                }

                return Map.of("recapVideoPath", videoPath != null ? videoPath : "");
        }

        private void unlockNextLesson(Long studentId, Lesson currentLesson) {

                List<Lesson> courseLessons = lessonRepository
                                .findByCourseIdOrderByLessonNumberAsc(currentLesson.getCourseId());

                courseLessons.stream()
                                .filter(l -> l.getLessonNumber() == currentLesson.getLessonNumber() + 1)
                                .findFirst()
                                .ifPresent(nextLesson -> lessonProgressRepository
                                                .findByStudentIdAndLessonId(studentId, nextLesson.getId())
                                                .ifPresent(nextProgress -> {
                                                        nextProgress.setIsLocked(false);
                                                        lessonProgressRepository.save(nextProgress);
                                                }));
        }

        private LessonProgressResponse toResponse(LessonProgress p) {
                return LessonProgressResponse.builder()
                                .id(p.getId())
                                .lessonId(p.getLessonId())
                                .isCompleted(Boolean.TRUE.equals(p.getIsCompleted()))
                                .isLocked(Boolean.TRUE.equals(p.getIsLocked()))
                                .quizPassed(Boolean.TRUE.equals(p.getQuizPassed()))
                                .build();
        }
}
