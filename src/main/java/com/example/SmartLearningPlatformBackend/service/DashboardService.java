package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.dashboard.DashboardResponse;
import com.example.SmartLearningPlatformBackend.dto.dashboard.DashboardResponse.*;
import com.example.SmartLearningPlatformBackend.models.*;
import com.example.SmartLearningPlatformBackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

        private final CourseRepository courseRepository;
        private final LessonRepository lessonRepository;
        private final LessonProgressRepository lessonProgressRepository;
        private final QuizRepository quizRepository;
        private final QuizAttemptRepository quizAttemptRepository;
        private final FlashcardRepository flashcardRepository;
        private final FlashcardReviewRepository flashcardReviewRepository;
        private final ExamRepository examRepository;
        private final ExamAttemptRepository examAttemptRepository;
        private final ActivityLogRepository activityLogRepository;

        @Transactional(readOnly = true)
        public DashboardResponse getDashboardData(Long studentId) {

                // ── 1. Courses ────────────────────────────────────────────────────────
                List<Course> courses = courseRepository.findByStudentId(studentId);

                int globalTotalLessons = 0;
                int globalCompletedLessons = 0;

                List<CourseProgressDto> courseProgressList = new ArrayList<>();

                for (Course course : courses) {
                        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByLessonNumberAsc(course.getId());
                        int totalLessons = lessons.size();
                        globalTotalLessons += totalLessons;

                        List<Long> lessonIds = lessons.stream().map(Lesson::getId).collect(Collectors.toList());

                        List<LessonProgress> progressList = lessonIds.isEmpty() ? List.of()
                                        : lessonProgressRepository.findByStudentIdAndLessonIdIn(studentId, lessonIds);

                        int completedLessons = (int) progressList.stream()
                                        .filter(p -> Boolean.TRUE.equals(p.getIsCompleted())).count();
                        int quizzesPassed = (int) progressList.stream()
                                        .filter(p -> Boolean.TRUE.equals(p.getQuizPassed())).count();
                        globalCompletedLessons += completedLessons;

                        int progressPercentage = 0;
                        if (totalLessons > 0) {
                                double lessonScore = (completedLessons / (double) totalLessons) * 50.0;
                                double quizScore = (quizzesPassed / (double) totalLessons) * 50.0;
                                progressPercentage = (int) Math.round(lessonScore + quizScore);
                        }

                        // Exam passed check
                        boolean examPassed = false;
                        Optional<Exam> exam = examRepository.findByCourseId(course.getId());
                        if (exam.isPresent()) {
                                examPassed = examAttemptRepository
                                                .existsByStudentIdAndExamIdAndIsPassed(studentId, exam.get().getId(),
                                                                true);
                        }

                        // Quiz count for this course
                        int totalQuizzes = (int) lessons.stream()
                                        .filter(l -> quizRepository.findByLessonId(l.getId()).isPresent())
                                        .count();

                        // Last accessed at
                        LocalDateTime lastAccessedAt = progressList.stream()
                                        .map(LessonProgress::getLastAccessedAt)
                                        .filter(Objects::nonNull)
                                        .max(Comparator.naturalOrder())
                                        .orElse(null);

                        courseProgressList.add(CourseProgressDto.builder()
                                        .courseId(course.getId())
                                        .title(course.getTitle())
                                        .category(course.getCategory())
                                        .progressPercentage(progressPercentage)
                                        .totalLessons(totalLessons)
                                        .completedLessons(completedLessons)
                                        .quizzesPassed(quizzesPassed)
                                        .totalQuizzes(totalQuizzes)
                                        .examPassed(examPassed)
                                        .lastAccessedAt(lastAccessedAt)
                                        .build());
                }

                int completedCourses = (int) courseProgressList.stream()
                                .filter(c -> c.getProgressPercentage() == 100).count();

                // ── 2. Global quiz statistics ─────────────────────────────────────────
                List<QuizAttempt> allAttempts = quizAttemptRepository.findByStudentId(studentId);
                int totalAttempts = allAttempts.size();
                int passedAttempts = (int) allAttempts.stream()
                                .filter(a -> Boolean.TRUE.equals(a.getIsPassed())).count();
                int averageScore = 0;
                if (totalAttempts > 0) {
                        averageScore = (int) Math.round(allAttempts.stream()
                                        .filter(a -> a.getScore() != null)
                                        .mapToInt(QuizAttempt::getScore)
                                        .average()
                                        .orElse(0.0));
                }

                // ── 3. Flashcards due today ───────────────────────────────────────────
                LocalDate today = LocalDate.now();
                List<FlashcardReview> dueReviews = flashcardReviewRepository
                                .findByStudentIdAndNextReviewDateLessThanEqual(studentId, today);
                int flashcardsDueToday = dueReviews.size();

                // Group due counts by course
                // Build flashcardId → courseId mapping
                Map<Long, Long> flashcardToCourse = new HashMap<>();
                Map<Long, String> courseIdToTitle = courses.stream()
                                .collect(Collectors.toMap(Course::getId, Course::getTitle));

                for (Course course : courses) {
                        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByLessonNumberAsc(course.getId());
                        for (Lesson lesson : lessons) {
                                flashcardRepository.findByLessonId(lesson.getId())
                                                .forEach(fc -> flashcardToCourse.put(fc.getId(), course.getId()));
                        }
                }

                // Total flashcards for this student
                int totalFlashcards = flashcardToCourse.size();

                // Due per course
                Map<Long, Integer> dueByCourseId = new LinkedHashMap<>();
                for (FlashcardReview r : dueReviews) {
                        Long cId = flashcardToCourse.get(r.getFlashcardId());
                        if (cId != null) {
                                dueByCourseId.merge(cId, 1, (a, b) -> a + b);
                        }
                }

                List<FlashcardsDueByCourseDto> flashcardsDueList = dueByCourseId.entrySet().stream()
                                .map(e -> FlashcardsDueByCourseDto.builder()
                                                .courseId(e.getKey())
                                                .courseTitle(courseIdToTitle.getOrDefault(e.getKey(), ""))
                                                .dueCount(e.getValue())
                                                .build())
                                .collect(Collectors.toList());

                // ── 4. Recent activity ────────────────────────────────────────────────
                List<ActivityDto> recentActivity = activityLogRepository
                                .findTop5ByUserIdOrderByTimestampDesc(studentId)
                                .stream()
                                .map(log -> ActivityDto.builder()
                                                .action(log.getAction().name())
                                                .description(log.getEntityType() != null
                                                                ? log.getAction().name().replace("_", " ") + " — "
                                                                                + log.getEntityType()
                                                                : log.getAction().name().replace("_", " "))
                                                .timestamp(log.getTimestamp())
                                                .build())
                                .collect(Collectors.toList());

                // ── Build response ────────────────────────────────────────────────────
                StatsDto stats = StatsDto.builder()
                                .totalCourses(courses.size())
                                .completedCourses(completedCourses)
                                .totalLessons(globalTotalLessons)
                                .completedLessons(globalCompletedLessons)
                                .totalQuizAttempts(totalAttempts)
                                .passedQuizAttempts(passedAttempts)
                                .averageQuizScore(averageScore)
                                .flashcardsDueToday(flashcardsDueToday)
                                .totalFlashcards(totalFlashcards)
                                .build();

                return DashboardResponse.builder()
                                .stats(stats)
                                .courses(courseProgressList)
                                .recentActivity(recentActivity)
                                .flashcardsDue(flashcardsDueList)
                                .build();
        }
}
