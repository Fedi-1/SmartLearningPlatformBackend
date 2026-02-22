package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.QuizAttemptQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptQuestionRepository extends JpaRepository<QuizAttemptQuestion, Long> {

    List<QuizAttemptQuestion> findByQuizAttemptId(Long quizAttemptId);
}
