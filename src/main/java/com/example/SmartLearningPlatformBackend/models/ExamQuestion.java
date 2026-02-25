package com.example.SmartLearningPlatformBackend.models;

import com.example.SmartLearningPlatformBackend.enums.DifficultyLevel;
import com.example.SmartLearningPlatformBackend.enums.QuestionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exam_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType;

    @Column(name = "correct_answer", nullable = false)
    private String correctAnswer;

    @Column(name = "option1")
    private String option1;

    @Column(name = "option2")
    private String option2;

    @Column(name = "option3")
    private String option3;

    @Column(name = "option4")
    private String option4;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private DifficultyLevel difficulty;

    @Column(name = "points_worth", nullable = false)
    private Integer pointsWorth;

    @Column(name = "section_number", nullable = false)
    private Integer sectionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Exam exam;
}
