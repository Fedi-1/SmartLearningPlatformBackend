package com.example.SmartLearningPlatformBackend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "lessons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "lesson_number", nullable = false)
    private Integer lessonNumber;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "estimated_read_time")
    private Integer estimatedReadTime;

    @Column(name = "recap_video_path")
    private String recapVideoPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Course course;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
