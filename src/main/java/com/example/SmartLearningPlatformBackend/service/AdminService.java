// src/main/java/.../service/AdminService.java
package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.admin.*;
import com.example.SmartLearningPlatformBackend.dto.student.ChangePasswordRequest;
import com.example.SmartLearningPlatformBackend.dto.student.StudentProfileResponse;
import com.example.SmartLearningPlatformBackend.dto.student.UpdateProfileRequest;
import com.example.SmartLearningPlatformBackend.enums.ActionType;
import com.example.SmartLearningPlatformBackend.enums.CertificateStatus;
import com.example.SmartLearningPlatformBackend.enums.NotificationCategory;
import com.example.SmartLearningPlatformBackend.enums.UserRole;
import com.example.SmartLearningPlatformBackend.models.ActivityLog;
import com.example.SmartLearningPlatformBackend.models.Certificate;
import com.example.SmartLearningPlatformBackend.models.Course;
import com.example.SmartLearningPlatformBackend.models.ExamAttempt;
import com.example.SmartLearningPlatformBackend.models.User;
import com.example.SmartLearningPlatformBackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

        private final UserRepository userRepository;
        private final CourseRepository courseRepository;
        private final CertificateRepository certificateRepository;
        private final DocumentRepository documentRepository;
        private final ExamAttemptRepository examAttemptRepository;
        private final ActivityLogRepository activityLogRepository;
        private final LessonProgressRepository lessonProgressRepository;
        private final QuizAttemptRepository quizAttemptRepository;
        private final FlashcardReviewRepository flashcardReviewRepository;
        private final ExamRepository examRepository;
        private final LessonRepository lessonRepository;
        private final NotificationService notificationService;
        private final SuspiciousActivityRepository suspiciousActivityRepository;
        private final PasswordEncoder passwordEncoder;

        @Transactional(readOnly = true)
        public AdminStatsResponse getStats() {
                try {
                        long totalStudents = userRepository.countByRole(UserRole.STUDENT);
                        long totalCourses = courseRepository.count();
                        long totalCertificates = certificateRepository.count();
                        long totalDocuments = documentRepository.count();

                        long total = examAttemptRepository.count();
                        long passed = examAttemptRepository.findAll().stream()
                                        .filter(a -> Boolean.TRUE.equals(a.getIsPassed()))
                                        .count();
                        int passRate = total == 0 ? 0 : (int) Math.round((double) passed / total * 100);

                        return AdminStatsResponse.builder()
                                        .totalStudents(totalStudents)
                                        .totalCourses(totalCourses)
                                        .totalCertificates(totalCertificates)
                                        .totalDocuments(totalDocuments)
                                        .examPassRate(passRate)
                                        .build();
                } catch (Exception e) {
                        log.error("AdminService.getStats failed", e);
                        throw e;
                }
        }

        @Transactional(readOnly = true)
        public List<ActivityChartPoint> getActivityChart(int days) {
                try {
                        LocalDateTime since = LocalDateTime.now().minusDays(days);
                        List<Object[]> rows = activityLogRepository.countByActionGroupedByDate("GENERATE_COURSE",
                                        since);
                        return rows.stream()
                                        .map(r -> new ActivityChartPoint(r[0].toString(), ((Number) r[1]).longValue()))
                                        .collect(Collectors.toList());
                } catch (Exception e) {
                        log.error("AdminService.getActivityChart failed", e);
                        return Collections.emptyList();
                }
        }

        @Transactional(readOnly = true)
        public List<CategoryDistributionPoint> getCategoryDistribution() {
                try {
                        return courseRepository.countGroupedByCategory().stream()
                                        .map(r -> new CategoryDistributionPoint((String) r[0],
                                                        ((Number) r[1]).longValue()))
                                        .collect(Collectors.toList());
                } catch (Exception e) {
                        log.error("AdminService.getCategoryDistribution failed", e);
                        return Collections.emptyList();
                }
        }

        @Transactional(readOnly = true)
        public List<RecentActivityEntry> getRecentActivity(int limit) {
                try {
                        DateTimeFormatter fmt = DateTimeFormatter.ISO_DATE_TIME;
                        List<ActivityLog> logs = activityLogRepository.findRecentWithUser(PageRequest.of(0, limit));
                        log.info("AdminService.getRecentActivity: fetched {} logs", logs.size());
                        return logs.stream()
                                        .map(log -> {
                                                String name = (log.getUser() != null)
                                                                ? log.getUser().getFirstName() + " "
                                                                                + log.getUser().getLastName()
                                                                : "Unknown";
                                                return new RecentActivityEntry(name, log.getAction().name(),
                                                                log.getTimestamp().format(fmt));
                                        })
                                        .collect(Collectors.toList());
                } catch (Exception e) {
                        log.error("AdminService.getRecentActivity failed", e);
                        return Collections.emptyList();
                }
        }

        // ── Students ──────────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<StudentSummaryResponse> getAllStudents() {
                DateTimeFormatter fmt = DateTimeFormatter.ISO_DATE_TIME;
                return userRepository.findByRole(UserRole.STUDENT).stream()
                                .map(user -> {
                                        int count = (int) courseRepository.findAll().stream()
                                                        .filter(c -> c.getStudentId().equals(user.getId())).count();
                                        int[] eng = computeEngagement(user.getId());
                                        String level = eng[0] >= 70 ? "HIGH" : eng[0] >= 40 ? "MEDIUM" : "LOW";
                                        return StudentSummaryResponse.builder()
                                                        .id(user.getId())
                                                        .firstName(user.getFirstName())
                                                        .lastName(user.getLastName())
                                                        .email(user.getEmail())
                                                        .isActive(user.getIsActive())
                                                        .createdAt(user.getCreatedAt() != null
                                                                        ? user.getCreatedAt().format(fmt)
                                                                        : null)
                                                        .lastLogin(user.getLastLogin() != null
                                                                        ? user.getLastLogin().format(fmt)
                                                                        : null)
                                                        .coursesCount(count)
                                                        .engagementScore(eng[0])
                                                        .engagementLevel(level)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public StudentDetailResponse getStudentDetail(Long studentId) {
                DateTimeFormatter fmt = DateTimeFormatter.ISO_DATE_TIME;
                User user = userRepository.findById(studentId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Student not found."));

                int[] eng = computeEngagement(studentId);
                String level = eng[0] >= 70 ? "HIGH" : eng[0] >= 40 ? "MEDIUM" : "LOW";

                List<Course> courses = courseRepository.findAll().stream()
                                .filter(c -> c.getStudentId().equals(studentId))
                                .collect(Collectors.toList());

                List<StudentCourseItem> courseItems = courses.stream().map(course -> {
                        List<Long> lessonIds = lessonRepository.findByCourseIdOrderByLessonNumberAsc(course.getId())
                                        .stream().map(l -> l.getId()).collect(Collectors.toList());
                        int totalLessons = lessonIds.size();
                        int completed = lessonIds.isEmpty() ? 0
                                        : (int) lessonProgressRepository
                                                        .findByStudentIdAndLessonIdIn(studentId, lessonIds)
                                                        .stream().filter(lp -> Boolean.TRUE.equals(lp.getIsCompleted()))
                                                        .count();

                        Boolean examPassed = null;
                        Integer examScore = null;
                        java.util.Optional<com.example.SmartLearningPlatformBackend.models.Exam> examOpt = examRepository
                                        .findByCourseId(course.getId());
                        if (examOpt.isPresent()) {
                                List<ExamAttempt> attempts = examAttemptRepository
                                                .findByStudentIdAndExamIdIn(studentId, List.of(examOpt.get().getId()));
                                if (!attempts.isEmpty()) {
                                        boolean anyPassed = attempts.stream()
                                                        .anyMatch(a -> Boolean.TRUE.equals(a.getIsPassed()));
                                        examPassed = anyPassed;
                                        examScore = attempts.stream()
                                                        .filter(a -> a.getScore() != null)
                                                        .mapToInt(ExamAttempt::getScore).max().orElse(0);
                                }
                        }

                        return StudentCourseItem.builder()
                                        .id(course.getId())
                                        .title(course.getTitle())
                                        .category(course.getCategory())
                                        .lessonsCompleted(completed)
                                        .totalLessons(totalLessons)
                                        .examPassed(examPassed)
                                        .examScore(examScore)
                                        .build();
                }).collect(Collectors.toList());

                List<StudentCertificateItem> certItems = certificateRepository.findAllByStudentId(studentId)
                                .stream().map(cert -> {
                                        String courseTitle = courseRepository.findById(cert.getCourseId())
                                                        .map(Course::getTitle).orElse("Unknown");
                                        return (StudentCertificateItem) StudentCertificateItem.builder()
                                                        .id(cert.getId())
                                                        .courseTitle(courseTitle)
                                                        .score(cert.getScore())
                                                        .issuedAt(cert.getIssuedAt() != null
                                                                        ? cert.getIssuedAt().toLocalDate().toString()
                                                                        : null)
                                                        .status(cert.getStatus())
                                                        .build();
                                }).collect(Collectors.toList());

                return StudentDetailResponse.builder()
                                .id(user.getId())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .email(user.getEmail())
                                .isActive(user.getIsActive())
                                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate().toString()
                                                : null)
                                .lastLogin(user.getLastLogin() != null ? user.getLastLogin().format(fmt) : null)
                                .engagementScore(eng[0])
                                .engagementLevel(level)
                                .courses(courseItems)
                                .certificates(certItems)
                                .build();
        }

        @Transactional(readOnly = true)
        public List<StudentExamAttemptItem> getStudentExamAttempts(Long studentId) {
                DateTimeFormatter fmt = DateTimeFormatter.ISO_DATE_TIME;
                return examAttemptRepository.findByStudentId(studentId).stream()
                                .map(attempt -> {
                                        String courseTitle = examRepository.findById(attempt.getExamId())
                                                        .flatMap(exam -> courseRepository.findById(exam.getCourseId()))
                                                        .map(Course::getTitle).orElse("Unknown");
                                        return StudentExamAttemptItem.builder()
                                                        .id(attempt.getId())
                                                        .courseTitle(courseTitle)
                                                        .score(attempt.getScore())
                                                        .isPassed(attempt.getIsPassed())
                                                        .attemptNumber(attempt.getAttemptNumber())
                                                        .submittedAt(attempt.getSubmittedAt() != null
                                                                        ? attempt.getSubmittedAt().format(fmt)
                                                                        : null)
                                                        .build();
                                }).collect(Collectors.toList());
        }

        @Transactional
        public boolean toggleStudentStatus(Long studentId) {
                User user = userRepository.findById(studentId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Student not found."));
                boolean newStatus = !Boolean.TRUE.equals(user.getIsActive());
                user.setIsActive(newStatus);
                userRepository.save(user);
                return newStatus;
        }

        // ── Certificate approval ──────────────────────────────────────────────────

        @Transactional
        public void approveCertificate(Long id) {
                Certificate cert = certificateRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Certificate not found."));
                if (cert.getStatus() == CertificateStatus.APPROVED) {
                        return;
                }
                cert.setStatus(CertificateStatus.APPROVED);
                certificateRepository.save(cert);
                String courseTitle = courseRepository.findById(cert.getCourseId())
                                .map(Course::getTitle).orElse("your course");
                notificationService.notify(
                                cert.getStudentId(),
                                NotificationCategory.CERTIFICATE,
                                "Certificate Approved 🏆",
                                String.format("Your certificate for \"%s\" has been approved. You can now generate and download it.",
                                                courseTitle),
                                cert.getId(),
                                "/dashboard/courses/" + cert.getCourseId());
        }

        @Transactional
        public void revokeCertificate(Long id) {
                Certificate cert = certificateRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Certificate not found."));
                if (cert.getStatus() == CertificateStatus.REVOKED) {
                        return;
                }
                cert.setStatus(CertificateStatus.REVOKED);
                certificateRepository.save(cert);
                String courseTitle = courseRepository.findById(cert.getCourseId())
                                .map(Course::getTitle).orElse("your course");
                notificationService.notify(
                                cert.getStudentId(),
                                NotificationCategory.CERTIFICATE,
                                "Certificate Revoked ❌",
                                String.format(
                                                "Your certificate for \"%s\" has been revoked due to suspicious activity detected during your exam. "
                                                                + "Focus on honest learning and try again.",
                                                courseTitle),
                                cert.getId(),
                                null);
        }

        // ── All Certificates (admin list) ─────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<AdminCertificateItem> getAllCertificates() {
                return certificateRepository.findAll().stream()
                                .sorted(Comparator.comparing(Certificate::getIssuedAt,
                                                Comparator.nullsLast(Comparator.reverseOrder())))
                                .map(cert -> {
                                        String studentName = "Unknown";
                                        String studentEmail = "";
                                        Optional<User> userOpt = userRepository.findById(cert.getStudentId());
                                        if (userOpt.isPresent()) {
                                                User u = userOpt.get();
                                                studentName = u.getFirstName() + " " + u.getLastName();
                                                studentEmail = u.getEmail();
                                        }
                                        String courseTitle = "";
                                        String category = "";
                                        Optional<Course> courseOpt = courseRepository.findById(cert.getCourseId());
                                        if (courseOpt.isPresent()) {
                                                courseTitle = courseOpt.get().getTitle();
                                                category = courseOpt.get().getCategory() != null
                                                                ? courseOpt.get().getCategory()
                                                                : "";
                                        }
                                        return AdminCertificateItem.builder()
                                                        .id(cert.getId())
                                                        .certificateUUID(cert.getCertificateUuid())
                                                        .studentName(studentName)
                                                        .studentEmail(studentEmail)
                                                        .studentId(cert.getStudentId())
                                                        .courseTitle(courseTitle)
                                                        .courseId(cert.getCourseId())
                                                        .category(category)
                                                        .score(cert.getScore())
                                                        .issuedAt(cert.getIssuedAt() != null
                                                                        ? cert.getIssuedAt().format(
                                                                                        DateTimeFormatter.ISO_LOCAL_DATE)
                                                                        : null)
                                                        .status(cert.getStatus())
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public CertificateVerifyResponse verifyCertificate(String uuid) {
                Optional<Certificate> certOpt = certificateRepository.findByCertificateUuid(uuid);
                if (certOpt.isEmpty()) {
                        return CertificateVerifyResponse.builder().valid(false).build();
                }
                Certificate cert = certOpt.get();
                String studentName = userRepository.findById(cert.getStudentId())
                                .map(u -> u.getFirstName() + " " + u.getLastName())
                                .orElse("Unknown");
                String courseTitle = courseRepository.findById(cert.getCourseId())
                                .map(Course::getTitle)
                                .orElse("");
                return CertificateVerifyResponse.builder()
                                .valid(true)
                                .studentName(studentName)
                                .courseTitle(courseTitle)
                                .score(cert.getScore())
                                .issuedAt(cert.getIssuedAt() != null
                                                ? cert.getIssuedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)
                                                : null)
                                .status(cert.getStatus())
                                .build();
        }

        // ── All Exam Attempts (admin list) ────────────────────────────────────────

        @Transactional(readOnly = true)
        public List<AdminExamAttemptItem> getAllExamAttempts() {
                DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                return examAttemptRepository.findAll().stream()
                                .filter(a -> a.getSubmittedAt() != null)
                                .sorted(Comparator.comparing(ExamAttempt::getSubmittedAt,
                                                Comparator.nullsLast(Comparator.reverseOrder())))
                                .map(attempt -> {
                                        String studentName = "Unknown";
                                        String studentEmail = "";
                                        Optional<User> userOpt = userRepository.findById(attempt.getStudentId());
                                        if (userOpt.isPresent()) {
                                                User u = userOpt.get();
                                                studentName = u.getFirstName() + " " + u.getLastName();
                                                studentEmail = u.getEmail();
                                        }
                                        String courseTitle = "";
                                        String category = "";
                                        Optional<Course> courseOpt = examRepository.findById(attempt.getExamId())
                                                        .flatMap(exam -> courseRepository.findById(exam.getCourseId()));
                                        if (courseOpt.isPresent()) {
                                                courseTitle = courseOpt.get().getTitle();
                                                category = courseOpt.get().getCategory() != null
                                                                ? courseOpt.get().getCategory()
                                                                : "";
                                        }
                                        int suspiciousCount = suspiciousActivityRepository
                                                        .totalCountForAttempt(attempt.getId());
                                        return AdminExamAttemptItem.builder()
                                                        .attemptId(attempt.getId())
                                                        .studentId(attempt.getStudentId())
                                                        .studentName(studentName)
                                                        .studentEmail(studentEmail)
                                                        .courseTitle(courseTitle)
                                                        .category(category)
                                                        .score(attempt.getScore())
                                                        .isPassed(attempt.getIsPassed())
                                                        .attemptNumber(attempt.getAttemptNumber())
                                                        .submittedAt(attempt.getSubmittedAt().format(fmt))
                                                        .hasSuspiciousActivity(suspiciousCount > 0)
                                                        .suspiciousEventsCount(suspiciousCount)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        // ── Admin profile ─────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public StudentProfileResponse getAdminProfile(Long userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                return StudentProfileResponse.builder()
                                .id(user.getId())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .email(user.getEmail())
                                .build();
        }

        @Transactional
        public StudentProfileResponse updateAdminProfile(Long userId, UpdateProfileRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
                        user.setFirstName(request.getFirstName());
                }
                if (request.getLastName() != null && !request.getLastName().isBlank()) {
                        user.setLastName(request.getLastName());
                }
                User saved = userRepository.save(user);
                return StudentProfileResponse.builder()
                                .id(saved.getId())
                                .firstName(saved.getFirstName())
                                .lastName(saved.getLastName())
                                .email(saved.getEmail())
                                .build();
        }

        @Transactional
        public void changeAdminPassword(Long userId, ChangePasswordRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                        throw new BadCredentialsException("Current password is incorrect");
                }
                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                userRepository.save(user);
        }

        // ── Private helpers ───────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public ActivityLogPageResponse getActivityLogs(int page, int size, String action, Long studentId) {
                ActionType actionType = (action != null && !action.isBlank()) ? ActionType.valueOf(action) : null;
                Page<ActivityLog> result = activityLogRepository.findFiltered(
                                actionType, studentId, PageRequest.of(page, size));
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                List<ActivityLogItem> items = result.getContent().stream().map(log -> {
                        String studentName = "";
                        String studentEmail = "";
                        Optional<User> userOpt = userRepository.findById(log.getUserId());
                        if (userOpt.isPresent()) {
                                studentName = userOpt.get().getFirstName() + " " + userOpt.get().getLastName();
                                studentEmail = userOpt.get().getEmail();
                        }
                        return ActivityLogItem.builder()
                                        .id(log.getId())
                                        .studentId(log.getUserId())
                                        .studentName(studentName)
                                        .studentEmail(studentEmail)
                                        .action(log.getAction().name())
                                        .entityType(log.getEntityType())
                                        .entityId(log.getEntityId())
                                        .timestamp(log.getTimestamp().format(fmt))
                                        .build();
                }).collect(Collectors.toList());
                return ActivityLogPageResponse.builder()
                                .content(items)
                                .totalElements(result.getTotalElements())
                                .totalPages(result.getTotalPages())
                                .currentPage(result.getNumber())
                                .build();
        }

        private int[] computeEngagement(Long studentId) {
                int lessons = lessonProgressRepository.countByStudentIdAndIsCompleted(studentId, true);
                int quizzesPassed = quizAttemptRepository.countByStudentIdAndIsPassed(studentId, true);
                int examsPassed = examAttemptRepository.countByStudentIdAndIsPassed(studentId, true);
                int flashcardsReviewed = flashcardReviewRepository.countByStudentId(studentId);
                int score = (lessons * 10) + (quizzesPassed * 15) + (examsPassed * 25)
                                + Math.min(flashcardsReviewed, 20);
                score = Math.min(score, 100);
                return new int[] { score };
        }
}
