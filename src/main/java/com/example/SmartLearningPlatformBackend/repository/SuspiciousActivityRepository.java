package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.enums.SuspiciousActivityType;
import com.example.SmartLearningPlatformBackend.models.SuspiciousActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuspiciousActivityRepository extends JpaRepository<SuspiciousActivity, Long> {

    Optional<SuspiciousActivity> findByExamAttemptIdAndActivityType(
            Long examAttemptId, SuspiciousActivityType activityType);

    List<SuspiciousActivity> findAllByExamAttemptId(Long examAttemptId);

    /**
     * Total number of individual events logged for one attempt (sum of all counts).
     */
    default int totalCountForAttempt(Long examAttemptId) {
        return findAllByExamAttemptId(examAttemptId).stream()
                .mapToInt(SuspiciousActivity::getCount)
                .sum();
    }

    /** Distinct attempt IDs that have at least one suspicious activity record. */
    @Query("SELECT DISTINCT s.examAttemptId FROM SuspiciousActivity s ORDER BY s.examAttemptId DESC")
    List<Long> findDistinctFlaggedAttemptIds();
}
