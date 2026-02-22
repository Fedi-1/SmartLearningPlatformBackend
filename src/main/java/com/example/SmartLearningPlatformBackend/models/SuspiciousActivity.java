package com.example.SmartLearningPlatformBackend.models;

import com.example.SmartLearningPlatformBackend.enums.SuspiciousActivityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "suspicious_activities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspiciousActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_attempt_id", nullable = false)
    private Long examAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private SuspiciousActivityType activityType;

    @Column(name = "count", nullable = false)
    @Builder.Default
    private Integer count = 1;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private LocalDateTime detectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_attempt_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ExamAttempt examAttempt;

    @PrePersist
    protected void onCreate() {
        detectedAt = LocalDateTime.now();
    }
}
