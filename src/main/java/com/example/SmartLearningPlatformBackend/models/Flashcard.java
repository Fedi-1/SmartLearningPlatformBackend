package com.example.SmartLearningPlatformBackend.models;

import com.example.SmartLearningPlatformBackend.enums.DifficultyLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "flashcards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flashcard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "term", nullable = false)
    private String term;

    @Column(name = "definition", columnDefinition = "TEXT", nullable = false)
    private String definition;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private DifficultyLevel difficulty;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Lesson lesson;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
