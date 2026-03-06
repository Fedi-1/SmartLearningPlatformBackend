package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.enums.NotificationCategory;
import com.example.SmartLearningPlatformBackend.models.Course;
import com.example.SmartLearningPlatformBackend.models.Lesson;
import com.example.SmartLearningPlatformBackend.models.LessonProgress;
import com.example.SmartLearningPlatformBackend.repository.CourseRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonProgressRepository;
import com.example.SmartLearningPlatformBackend.repository.LessonRepository;
import com.example.SmartLearningPlatformBackend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderSchedulerService {

    /** Remind students who haven't touched a course in this many days. */
    private static final int INACTIVITY_DAYS = 3;

    /**
     * Don't send a reminder for the same course more than once per this many days.
     */
    private static final int DEDUP_DAYS = 7;

    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    /**
     * Catch-up run on startup — in case the backend was down at 9:00 AM.
     * Runs once, 10 seconds after the application is fully ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(initialDelay = 10_000, fixedDelay = Long.MAX_VALUE)
    public void runOnStartup() {
        log.info("[ReminderScheduler] Catch-up run on startup");
        sendInactivityReminders();
    }

    /**
     * Runs every day at 09:00.
     * Finds students with in-progress courses they haven't visited in 3+ days
     * and sends them a reminder email (once per course per 7 days).
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendInactivityReminders() {
        log.info("[ReminderScheduler] Starting daily inactivity reminder run");

        LocalDateTime cutoff = LocalDateTime.now().minusDays(INACTIVITY_DAYS);

        // Fetch all inactive in-progress LessonProgress records
        List<LessonProgress> staleRecords = lessonProgressRepository
                .findInactiveInProgressLessons(cutoff);

        if (staleRecords.isEmpty()) {
            log.info("[ReminderScheduler] No inactive students found.");
            return;
        }

        // Pre-load all relevant lessons to resolve courseId, keyed by lessonId
        List<Long> lessonIds = staleRecords.stream()
                .map(LessonProgress::getLessonId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Lesson> lessonMap = lessonRepository.findAllById(lessonIds)
                .stream()
                .collect(Collectors.toMap(Lesson::getId, l -> l));

        // Pre-load all relevant courses, keyed by courseId
        List<Long> courseIds = lessonMap.values().stream()
                .map(Lesson::getCourseId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Course> courseMap = courseRepository.findAllById(courseIds)
                .stream()
                .collect(Collectors.toMap(Course::getId, c -> c));

        // Deduplicate: one reminder per (studentId, courseId) pair per run
        Map<String, Boolean> sentThisRun = new HashMap<>();
        LocalDateTime dedupSince = LocalDateTime.now().minusDays(DEDUP_DAYS);

        int sent = 0;
        for (LessonProgress lp : staleRecords) {
            Lesson lesson = lessonMap.get(lp.getLessonId());
            if (lesson == null)
                continue;

            Long studentId = lp.getStudentId();
            Long courseId = lesson.getCourseId();
            String key = studentId + ":" + courseId;

            // Skip if already sent this run for this (student, course) pair
            if (sentThisRun.containsKey(key))
                continue;
            sentThisRun.put(key, true);

            // Skip if a reminder was already sent for this course within the dedup window
            boolean alreadySent = notificationRepository
                    .existsByUserIdAndCategoryAndReferenceIdAndSentAtAfter(
                            studentId, NotificationCategory.REMINDER, courseId, dedupSince);
            if (alreadySent) {
                log.debug("[ReminderScheduler] Skipping student={} course={} — reminder already sent recently",
                        studentId, courseId);
                continue;
            }

            Course course = courseMap.get(courseId);
            String courseTitle = course != null ? course.getTitle() : "your course";

            notificationService.notify(
                    studentId,
                    NotificationCategory.REMINDER,
                    "Don't lose your streak! 📚",
                    String.format(
                            "You haven't studied \"%s\" in %d days. Pick up where you left off and keep the momentum going!",
                            courseTitle, INACTIVITY_DAYS),
                    courseId,
                    "/dashboard/courses/" + courseId);

            sent++;
            log.info("[ReminderScheduler] Sent reminder to student={} for course=\"{}\"", studentId, courseTitle);
        }

        log.info("[ReminderScheduler] Done — {} reminder(s) sent.", sent);
    }
}
