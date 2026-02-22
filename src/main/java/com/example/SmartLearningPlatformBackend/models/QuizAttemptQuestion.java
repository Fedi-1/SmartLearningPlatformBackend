package com.example.SmartLearningPlatformBackend.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_attempt_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttemptQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_attempt_id", nullable = false)
    private Long quizAttemptId;

    @Column(name = "quiz_question_id", nullable = false)
    private Long quizQuestionId;
}
