package com.example.SmartLearningPlatformBackend.service;

import com.example.SmartLearningPlatformBackend.dto.exam.LogSuspiciousActivityRequest;
import com.example.SmartLearningPlatformBackend.dto.exam.SuspiciousActivityDTO;
import com.example.SmartLearningPlatformBackend.enums.SuspiciousActivityType;
import com.example.SmartLearningPlatformBackend.models.ExamAttempt;
import com.example.SmartLearningPlatformBackend.models.SuspiciousActivity;
import com.example.SmartLearningPlatformBackend.repository.ExamAttemptRepository;
import com.example.SmartLearningPlatformBackend.repository.SuspiciousActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuspiciousActivityService {

    private final SuspiciousActivityRepository suspiciousActivityRepository;
    private final ExamAttemptRepository examAttemptRepository;

    /**
     * Log or increment a suspicious activity for an active exam attempt.
     * Uses upsert: if the same activityType already exists for this attempt,
     * increment its count; otherwise insert a new row.
     */
    @Transactional
    public SuspiciousActivityDTO logActivity(Long attemptId, Long studentId,
            LogSuspiciousActivityRequest request) {

        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Exam attempt not found."));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new RuntimeException("Access denied.");
        }

        SuspiciousActivityType type = request.getActivityType();

        // Upsert: increment count if row already exists, else create it
        SuspiciousActivity activity = suspiciousActivityRepository
                .findByExamAttemptIdAndActivityType(attemptId, type)
                .map(existing -> {
                    existing.setCount(existing.getCount() + 1);
                    return suspiciousActivityRepository.save(existing);
                })
                .orElseGet(() -> suspiciousActivityRepository.save(
                        SuspiciousActivity.builder()
                                .examAttemptId(attemptId)
                                .activityType(type)
                                .count(1)
                                .build()));

        int total = suspiciousActivityRepository.totalCountForAttempt(attemptId);

        return toDTO(activity, total);
    }

    /** Return all suspicious activities for one attempt (student or admin). */
    public List<SuspiciousActivityDTO> getForAttempt(Long attemptId, Long studentId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Exam attempt not found."));

        if (!attempt.getStudentId().equals(studentId)) {
            throw new RuntimeException("Access denied.");
        }

        int total = suspiciousActivityRepository.totalCountForAttempt(attemptId);
        return suspiciousActivityRepository.findAllByExamAttemptId(attemptId)
                .stream()
                .map(a -> toDTO(a, total))
                .collect(Collectors.toList());
    }

    /**
     * Admin: return all suspicious activities for any attempt (no ownership check).
     */
    public List<SuspiciousActivityDTO> getForAttemptAdmin(Long attemptId) {
        int total = suspiciousActivityRepository.totalCountForAttempt(attemptId);
        return suspiciousActivityRepository.findAllByExamAttemptId(attemptId)
                .stream()
                .map(a -> toDTO(a, total))
                .collect(Collectors.toList());
    }

    /**
     * Admin: return all distinct attempt IDs that have at least one suspicious
     * activity.
     */
    public List<Long> getFlaggedAttemptIds() {
        return suspiciousActivityRepository.findDistinctFlaggedAttemptIds();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private SuspiciousActivityDTO toDTO(SuspiciousActivity a, int total) {
        return SuspiciousActivityDTO.builder()
                .id(a.getId())
                .examAttemptId(a.getExamAttemptId())
                .activityType(a.getActivityType())
                .count(a.getCount())
                .detectedAt(a.getDetectedAt())
                .totalCount(total)
                .build();
    }
}
