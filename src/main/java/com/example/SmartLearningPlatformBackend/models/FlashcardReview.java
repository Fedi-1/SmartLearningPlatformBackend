package com.example.SmartLearningPlatformBackend.models;

import com.example.SmartLearningPlatformBackend.enums.FlashcardRating;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "flashcard_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlashcardReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "flashcard_id", nullable = false)
    private Long flashcardId;

    @Column(name = "ease_factor", nullable = false)
    @Builder.Default
    private Float easeFactor = 2.5f;

    @Column(name = "interval", nullable = false)
    @Builder.Default
    private Integer interval = 1;

    @Column(name = "repetition_count", nullable = false)
    @Builder.Default
    private Integer repetitionCount = 0;

    @Column(name = "next_review_date", nullable = false)
    private LocalDate nextReviewDate;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_rating")
    private FlashcardRating lastRating;

    @Column(name = "quality_score")
    private Integer qualityScore;

    @Column(name = "consecutive_correct_reviews", nullable = false)
    @Builder.Default
    private Integer consecutiveCorrectReviews = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flashcard_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Flashcard flashcard;
}
