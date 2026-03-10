package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    List<QuizAnswer> findByQuizAttemptId(Long quizAttemptId);

    @Modifying
    @Query("DELETE FROM QuizAnswer qa WHERE qa.quizAttemptId IN :attemptIds")
    void deleteAllByQuizAttemptIdIn(List<Long> attemptIds);
}
