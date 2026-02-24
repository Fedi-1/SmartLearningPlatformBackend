package com.example.SmartLearningPlatformBackend.repository;

import com.example.SmartLearningPlatformBackend.models.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByStudentIdAndQuizId(Long studentId, Long quizId);

    int countByStudentIdAndQuizId(Long studentId, Long quizId);

    List<QuizAttempt> findByStudentId(Long studentId);

    List<QuizAttempt> findByStudentIdAndQuizIdIn(Long studentId, List<Long> quizIds);
}
