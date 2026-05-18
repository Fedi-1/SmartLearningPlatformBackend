package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.QuizAttemptQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptQuestionRepository extends JpaRepository<QuizAttemptQuestion, Long> {

    List<QuizAttemptQuestion> findByQuizAttemptId(Long quizAttemptId);

    List<QuizAttemptQuestion> findByQuizAttemptIdIn(List<Long> quizAttemptIds);

    @Modifying
    @Query("DELETE FROM QuizAttemptQuestion qq WHERE qq.quizAttemptId IN :attemptIds")
    void deleteAllByQuizAttemptIdIn(List<Long> attemptIds);
}
