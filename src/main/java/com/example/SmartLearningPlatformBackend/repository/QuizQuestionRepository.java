package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.enums.QuestionType;
import com.example.SmartLearningPlatformBackend.models.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findByQuizIdOrderByQuestionNumberAsc(Long quizId);

    List<QuizQuestion> findByQuizIdAndQuestionType(Long quizId, QuestionType questionType);
}
