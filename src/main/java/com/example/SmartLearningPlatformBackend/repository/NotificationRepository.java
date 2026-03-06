package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.enums.NotificationCategory;
import com.example.SmartLearningPlatformBackend.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderBySentAtDesc(Long userId);

    List<Notification> findByUserIdAndIsReadFalse(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * Dedup check: has a reminder already been sent to this user for this course
     * recently?
     */
    boolean existsByUserIdAndCategoryAndReferenceIdAndSentAtAfter(
            Long userId, NotificationCategory category, Long referenceId, LocalDateTime since);
}
