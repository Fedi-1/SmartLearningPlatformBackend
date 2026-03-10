package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

        List<ActivityLog> findTop5ByUserIdOrderByTimestampDesc(Long userId);

        @Query("SELECT a FROM ActivityLog a LEFT JOIN FETCH a.user ORDER BY a.timestamp DESC")
        List<ActivityLog> findRecentWithUser(org.springframework.data.domain.Pageable pageable);

        @Query(value = "SELECT CAST(a.timestamp AS date) AS day, COUNT(a.id) AS cnt " +
                        "FROM activity_logs a " +
                        "WHERE a.action = :action AND a.timestamp >= :since " +
                        "GROUP BY CAST(a.timestamp AS date) " +
                        "ORDER BY CAST(a.timestamp AS date) ASC", nativeQuery = true)
        List<Object[]> countByActionGroupedByDate(@Param("action") String action,
                        @Param("since") LocalDateTime since);

        @Query("SELECT a FROM ActivityLog a WHERE " +
                        "(:action IS NULL OR a.action = :action) AND " +
                        "(:studentId IS NULL OR a.userId = :studentId) " +
                        "ORDER BY a.timestamp DESC")
        Page<ActivityLog> findFiltered(
                        @Param("action") com.example.SmartLearningPlatformBackend.enums.ActionType action,
                        @Param("studentId") Long studentId,
                        Pageable pageable);
}
